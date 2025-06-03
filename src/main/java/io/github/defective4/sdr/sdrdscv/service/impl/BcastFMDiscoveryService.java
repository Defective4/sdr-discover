package io.github.defective4.sdr.sdrdscv.service.impl;

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
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation.Modulation;
import io.github.defective4.sdr.sdrdscv.radio.tuner.Tuner;
import io.github.defective4.sdr.sdrdscv.service.ServiceArgument;
import io.github.defective4.sdr.sdrdscv.tool.GRScriptRunner;

public class BcastFMDiscoveryService implements DiscoveryService {

    public static class Builder extends DiscoveryServiceBuilder {
        private boolean automaticStep = true, detectStereo = true;
        private float controlProbeFrequency = -1;
        private long controlProbeLength = 2500;
        private int gain = 0, rdsPort = 25555, probePort = 25556, ctlPort = 25557;
        private boolean offsetTuning = true;
        private long probeTimeout = 10000;
        private long rdsRecvTime = 10000;
        private String sdrParams = "";
        private double sensitivity = 0;
        private float startFreq = 87e6f, endFreq = 108e6f;
        private StationNameConflictMode stationNameConflictMode = StationNameConflictMode.SMART;
        private long tuningDelay = 200;

        @Override
        public BcastFMDiscoveryService build() {
            return new BcastFMDiscoveryService(sdrParams, gain, rdsPort, probePort, ctlPort, controlProbeFrequency,
                    controlProbeLength, sensitivity, startFreq, endFreq, probeTimeout, rdsRecvTime,
                    stationNameConflictMode, automaticStep, detectStereo, offsetTuning, verbose, tuningDelay);
        }

        @ServiceArgument(argName = "disable-offset-tuning", description = "Disable offset tuning - always tune to the center of the signal")
        public Builder disableOffsetTuning() {
            offsetTuning = false;
            return this;
        }

        @ServiceArgument(argName = "control-port", description = "Control port for GNU Radio communication", defaultField = "ctlPort")
        public Builder withControlPort(int ctlPort) {
            this.ctlPort = ctlPort;
            return this;
        }

        @ServiceArgument(defaultField = "controlProbeLength", argName = "control-probe-duration", description = "How long (ms) should a control probe last? Only used if control-probe-freq is set")
        public Builder withControlProbeDuration(long controlProbeDuration) {
            controlProbeLength = controlProbeDuration;
            return this;
        }

        @ServiceArgument(argName = "control-probe-frequency", description = "Control probe frequency (Hz). If this option is enabled, the discovery service will first probe signal on the specified frequency, then use it as a reference point to detect other stations. It's recommended to set this option to frequency of the weakest radio station in your area")
        public Builder withControlProbeFrequency(float controlProbeFrequency) {
            this.controlProbeFrequency = controlProbeFrequency;
            return this;
        }

        @ServiceArgument(defaultField = "endFreq", argName = "end-frequency", description = "Maximum scanning frequency")
        public Builder withEndFrequency(float endFreq) {
            this.endFreq = endFreq;
            return this;
        }

        @ServiceArgument(defaultField = "gain", argName = "gain", description = "RF Gain")
        public Builder withGain(int gain) {
            this.gain = gain;
            return this;
        }

        @ServiceArgument(argName = "no-automatic-step", description = "Disable automatic step correction after detecting a station. Disabling this setting might cause some stations to get detected twice.")
        public Builder withNoAutomaticStep() {
            automaticStep = false;
            return this;
        }

        @ServiceArgument(argName = "disable-stereo", description = "Disable stereo detection. All detected radio station will be marked as mono")
        public Builder withNoDetectStereo() {
            detectStereo = false;
            return this;
        }

        @ServiceArgument(defaultField = "probePort", argName = "probe-port", description = "Probe port for GNU Radio communication")
        public Builder withProbePort(int probePort) {
            this.probePort = probePort;
            return this;
        }

        @ServiceArgument(defaultField = "probeInterval", argName = "probe-timeout", description = "How long (ms) should we wait for a signal probe?")
        public Builder withProbeTimeout(long probeTimeout) {
            this.probeTimeout = probeTimeout;
            return this;
        }

        @ServiceArgument(defaultField = "rdsPort", argName = "rds-port", description = "RDS port for GNU Radio communication")
        public Builder withRDSPort(int rdsPort) {
            this.rdsPort = rdsPort;
            return this;
        }

        @ServiceArgument(defaultField = "rdsRecvTime", argName = "rds-receive-time", description = "How long (ms) should we wait for RDS data. Longer values may improve the quality of detected data, but will make discovery take a significantly longer amount of time")
        public Builder withRDSReceiveTime(long rdsRecvTime) {
            this.rdsRecvTime = rdsRecvTime;
            return this;
        }

        @ServiceArgument(argName = "sdr-params", description = "Params to pass to the SDR driver")
        public Builder withSDRParams(String sdrParams) {
            this.sdrParams = sdrParams;
            return this;
        }

        @ServiceArgument(defaultField = "sensitivty", argName = "sensitivity", description = "Manually specify receiver sensitivity. Values between 0 - 10 are recommended. Higher values mean lower sensitivity, and values > 10 turn the receiver practically deaf.")
        public Builder withSensitivity(double sensitivity) {
            this.sensitivity = sensitivity;
            return this;
        }

        @ServiceArgument(defaultField = "startFreq", argName = "start-frequency", description = "Scan start frequency")
        public Builder withStartFrequency(float startFreq) {
            this.startFreq = startFreq;
            return this;
        }

        @ServiceArgument(defaultField = "stationNameConflictMode", argName = "station-name-conflict-mode", description = "How to resolve name conflicts? \"Smart\" will try to detect the most commonly transmitted station name. \"Merge\" will remove duplicates and merge all received station names into one.")
        public Builder withStationNameConflictMode(StationNameConflictMode stationNameConflictMode) {
            this.stationNameConflictMode = stationNameConflictMode;
            return this;
        }

        @ServiceArgument(argName = "tuning-delay", description = "A delay (ms) between each tuning attempt. Higher values might help with detecting new stations, but they will make the scan take longer time", defaultField = "tuningDelay")
        public Builder withTuningDelay(long delay) {
            tuningDelay = delay;
            return this;
        }

    }

    public static enum StationNameConflictMode {
        MERGE, SMART;
    }

    private final boolean automaticStep, detectStereo;
    private final float controlProbeFrequency;
    private final long controlProbeLength;
    private final int gain, rdsPort, probePort, ctlPort;
    private final boolean offsetTuning;
    private final long probeTimeout;
    private Process process;
    private final long rdsRecvTime;
    private final String sdrParams;
    private final double sensitivity;
    private final float startFreq, endFreq;
    private final StationNameConflictMode stationNameConflictMode;
    private Tuner tuner;
    private final long tuningDelay;
    private final boolean verbose;

    private BcastFMDiscoveryService(String sdrParams, int gain, int rdsPort, int probePort, int ctlPort,
            float controlProbeFrequency, long controlProbeLength, double sensitivity, float startFreq, float endFreq,
            long probeTimeout, long rdsRecvTime, StationNameConflictMode stationNameConflictMode, boolean automaticStep,
            boolean detectStereo, boolean offsetTuning, boolean verbose, long tuningDelay) {
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
        this.offsetTuning = offsetTuning;
        this.verbose = verbose;
        this.tuningDelay = tuningDelay;
    }

    @Override
    public List<RadioStation> discover() throws Exception {
        process = GRScriptRunner
                .run("rds_rx.py", Collections.singleton("rds_rx_epy_block_0.py"), "--params", sdrParams, "--gain", gain,
                        "--rdsPort", rdsPort, "--probePort", probePort, "--ctlPort", ctlPort);
        try (RawMessageReceiver signalProbe = new RawMessageReceiver("tcp://localhost:" + probePort, false);
                RawMessageSender controller = new RawMessageSender("tcp://localhost:" + ctlPort, false);
                RDSReceiver rdsReceiver = new RDSReceiver("tcp://localhost:" + rdsPort, false)) {
            tuner = DiscoveryService.createTuner(controller, offsetTuning);
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
                Collection<Double> avgCol = measureAverageSignalStrength(signalProbe);
                average = avgCol.stream().mapToDouble(e -> e).average().orElse(-1);
                if (average < 0) throw new IOException("Failed to calculate average signal");
                if (verbose) System.err
                        .println("Average signal strength on the control frequency is " + average + " based on "
                                + avgCol.size() + " samples.");
            } else average = sensitivity / 100d;

            List<RadioStation> stations = new ArrayList<>();
            for (float freq = startFreq; freq <= endFreq; freq += 100e3f) {
                if (verbose) System.err.println("Trying " + freq + "Hz...");
                RadioStation station = tryFrequency(freq, signalProbe, rdsReceiver, average);
                if (station != null) {
                    System.err.println("Detected a new station \"" + station.getName() + "\" on " + freq + "Hz");
                    if (automaticStep) freq += 100e3f;
                    stations.add(station);
                }
            }
            return Collections.unmodifiableList(stations);
        }
    }

    private Collection<Double> measureAverageSignalStrength(RawMessageReceiver probe)
            throws InterruptedException, IOException {
        if (!process.isAlive()) throw new IOException("The received died");
        tuner.tune(controlProbeFrequency);
        Thread.sleep(tuningDelay);
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

    private RadioStation tryFrequency(float freq, RawMessageReceiver probe, RDSReceiver rdsReceiver, double threshold)
            throws InterruptedException, IOException {
        if (!process.isAlive()) throw new IOException("The received died");
        tuner.tune(freq);
        Thread.sleep(tuningDelay);
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
                String radioText = radiotexts.isEmpty() ? null : resolveDuplicates(radiotexts);
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
