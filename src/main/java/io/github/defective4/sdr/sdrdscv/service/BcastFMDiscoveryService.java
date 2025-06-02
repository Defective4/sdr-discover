package io.github.defective4.sdr.sdrdscv.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.defective4.sdr.msg.MessageListener;
import io.github.defective4.sdr.msg.MessagePair;
import io.github.defective4.sdr.msg.RawMessageReceiver;
import io.github.defective4.sdr.msg.RawMessageSender;
import io.github.defective4.sdr.rds.RDSAdapter;
import io.github.defective4.sdr.rds.RDSFlags;
import io.github.defective4.sdr.rds.RDSListener;
import io.github.defective4.sdr.rds.RDSReceiver;
import io.github.defective4.sdr.sdrdscv.RadioStation;
import io.github.defective4.sdr.sdrdscv.RadioStation.Modulation;
import io.github.defective4.sdr.sdrdscv.tool.GRScriptRunner;

public class BcastFMDiscoveryService implements DiscoveryService {

    public static enum StationNameConflictMode {
        MERGE, SMART;
    }

    private final boolean automaticStep, detectStereo;
    private final float controlProbeFrequency;
    private final long controlProbeLength;
    private final int gain, rdsPort, probePort, ctlPort;
    private final long probeTimeout;
    private Process process;
    private final long rdsRecvTime;
    private final String sdrParams;
    private final double sensitivity;
    private final float startFreq, endFreq;
    private final StationNameConflictMode stationNameConflictMode;
    private final boolean verbose = true; // TODO

    public BcastFMDiscoveryService(String sdrParams, int gain, int rdsPort, int probePort, int ctlPort,
            float controlProbeFrequency, long controlProbeLength, double sensitivity, float startFreq, float endFreq,
            long probeTimeout, long rdsRecvTime, StationNameConflictMode stationNameConflictMode, boolean automaticStep,
            boolean detectStereo) {
        this.sdrParams = sdrParams;
        this.gain = gain;
        this.rdsPort = rdsPort;
        this.probePort = probePort;
        this.ctlPort = ctlPort;
        this.controlProbeFrequency = controlProbeFrequency;
        this.controlProbeLength = controlProbeLength;
        this.sensitivity = sensitivity;
        this.startFreq = startFreq;
        this.endFreq = endFreq;
        this.probeTimeout = probeTimeout;
        this.rdsRecvTime = rdsRecvTime;
        this.stationNameConflictMode = stationNameConflictMode;
        this.automaticStep = automaticStep;
        this.detectStereo = detectStereo;
    }

    @Override
    public List<RadioStation> discover() throws Exception {
        process = GRScriptRunner
                .run("rds_rx.py", Collections.singleton("rds_rx_epy_block_0.py"), "--params", sdrParams, "--gain", gain,
                        "--rdsPort", rdsPort, "--probePort", probePort, "--ctlPort", ctlPort);
        try (RawMessageReceiver signalProbe = new RawMessageReceiver("tcp://localhost:" + probePort, false);
                RawMessageSender controller = new RawMessageSender("tcp://localhost:" + ctlPort, false);
                RDSReceiver rdsReceiver = new RDSReceiver("tcp://localhost:" + rdsPort, false)) {
            rdsReceiver.setAllowDuplicateStationUpdates(true);
            rdsReceiver.setAllowDuplicateRadiotextUpdates(true);
            controller.start();
            new Thread(() -> {
                try {
                    signalProbe.start();
                } catch (Exception e) {}
            }).start();
            new Thread(() -> {
                try {
                    rdsReceiver.start();
                } catch (Exception e2) {}
            }).start();

            double average;
            if (controlProbeFrequency != -1) {
                if (verbose) {
                    System.err
                            .println("Probing average signal strength on the control frequency " + controlProbeFrequency
                                    + "Hz...");
                }
                Collection<Double> avgCol = measureAverageSignalStrength(controller, signalProbe);
                average = avgCol.stream().mapToDouble(e -> e).average().orElse(-1);
                if (average < 0) throw new IOException("Failed to calculate average signal");
                if (verbose) System.err
                        .println("Average signal strength on the control frequency is " + average + " based on "
                                + avgCol.size() + " samples.");
            } else average = sensitivity / 100d;

            List<RadioStation> stations = new ArrayList<>();
            for (float freq = startFreq; freq <= endFreq; freq += 100e3f) {
                if (verbose) System.err.println("Trying " + freq + "Hz...");
                RadioStation station = tryFrequency(freq, controller, signalProbe, rdsReceiver, average);
                if (station != null) {
                    System.err.println("Detected a new station \"" + station.getName() + "\" on " + freq + "Hz");
                    if (automaticStep) freq += 100e3f;
                }
            }
            return Collections.unmodifiableList(stations);
        }
    }

    private Collection<Double> measureAverageSignalStrength(RawMessageSender controller, RawMessageReceiver probe)
            throws InterruptedException, IOException {
        if (!process.isAlive()) throw new IOException("The received died");
        controller.sendMessage(new MessagePair("freq", (double) controlProbeFrequency));
        Thread.sleep(100);
        List<Double> measures = new ArrayList<>();
        MessageListener probeListener = new MessageListener() {
            @Override
            public void messageReceived(MessagePair pair) {
                measures.add(pair.getAsDouble());
            }
        };
        probe.addListener(probeListener);
        Thread.sleep(controlProbeLength);
        probe.removeListener(probeListener);
        return Collections.unmodifiableCollection(measures);
    }

    private RadioStation tryFrequency(float freq, RawMessageSender controller, RawMessageReceiver probe,
            RDSReceiver rdsReceiver, double threshold) throws InterruptedException, IOException {
        if (!process.isAlive()) throw new IOException("The received died");
        controller.sendMessage(new MessagePair("freq", (double) freq));
        Thread.sleep(100);
        AtomicReference<Double> signalRef = new AtomicReference<>();
        MessageListener probeListener = new MessageListener() {
            @Override
            public void messageReceived(MessagePair pair) {
                signalRef.set(pair.getAsDouble());
            }
        };
        probe.addListener(probeListener);
        long start = System.currentTimeMillis();
        while (signalRef.get() == null && System.currentTimeMillis() - start < probeTimeout) {
            Thread.sleep(100);
        }
        probe.removeListener(probeListener);
        if (signalRef.get() == null) throw new IOException("Couldn't probe signal on " + freq + "Hz");
        if (signalRef.get() > threshold) {
            if (verbose) System.err.println("Locked. Waiting for RDS...");

            List<String> stationNames = new ArrayList<>();
            AtomicReference<String> programInfoRef = new AtomicReference<>();
            List<String> radiotexts = new ArrayList<>();
            AtomicBoolean stereo = new AtomicBoolean();

            RDSListener rdsLs = new RDSAdapter() {
                @Override
                public void flagsUpdated(RDSFlags flags) {
                    stereo.set(flags.isStereo());
                }

                @Override
                public void programInfoUpdated(String programInfo) {
                    programInfoRef.set(programInfo);
                }

                @Override
                public void radiotextUpdated(String radiotext) {
                    radiotexts.add(radiotext);
                }

                @Override
                public void stationUpdated(String station) {
                    stationNames.add(station);
                }
            };
            rdsReceiver.addListener(rdsLs);
            Thread.sleep(rdsRecvTime);
            String pi = programInfoRef.get();
            if (pi == null) {
                if (verbose) System.err.println("No RDS data received. Skipped.");
            } else if (stationNames.isEmpty()) {
                if (verbose) System.err.println("Received RDS data, but the station didn't identify itself. Skipped.");
            } else {
                String stationName;
                switch (stationNameConflictMode) {
                    default:
                    case SMART: {
                        stationName = resolveDuplicates(stationNames);
                        break;
                    }
                    case MERGE: {
                        List<String> deduped = new ArrayList<>();
                        for (String n : stationNames) if (!deduped.contains(n)) deduped.add(n);
                        stationName = String.join("/", deduped.toArray(new String[0]));
                        break;
                    }
                }
                String radioText = resolveDuplicates(radiotexts);
                return new RadioStation(stationName, radioText, freq,
                        detectStereo && stereo.get() ? Modulation.WFM_STEREO : Modulation.WFM);
            }
        }
        return null;
    }

    private static String resolveDuplicates(Collection<String> collection) {
        if (collection.isEmpty()) throw new IllegalArgumentException("collection can't be empty");
        Map<String, Integer> stringRate = new HashMap<>();
        for (String string : collection) {
            stringRate.compute(string, (key, val) -> (val == null ? 0 : val) + 1);
        }
        return stringRate
                .entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                .findFirst()
                .get()
                .getKey();
    }
}
