package io.github.defective4.sdr.sdrdscv.service.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.defective4.sdr.msg.RawMessageSender;
import io.github.defective4.sdr.sdrdscv.annotation.BuilderParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.radio.tuner.Tuner;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryService;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryServiceBuilder;
import io.github.defective4.sdr.sdrdscv.service.decorator.impl.ChainServiceDecorator;
import io.github.defective4.sdr.sdrdscv.tool.GRScriptRunner;

public class FFTDiscoveryService implements DiscoveryService {

    public static class Builder extends DiscoveryServiceBuilder<FFTDiscoveryService> {

        private int controlPort = 25555;
        private boolean dcBlock = true;
        private float dcFilterWidth = 10e3f;
        private float endFreq = 108e6f;
        private int fftSize = 1024;
        private boolean probe = false;
        private String probeCsvFile = null;
        private int rxPort = 2000;
        private float startFreq = 88e6f;
        private int tuneDelay = 1000;

        @Override
        public FFTDiscoveryService build() throws Exception {
            return new FFTDiscoveryService(fftSize, controlPort, endFreq, startFreq, verbose, rxPort, tuneDelay,
                    dcBlock, dcFilterWidth);
        }

        @BuilderParam(argName = "dc-block", description = "Tries to ignore the DC spike.")
        public Builder dcBlock() {
            dcBlock = true;
            return this;
        }

        @BuilderParam(argName = "probe", description = "Enables FFT probe mode. In pobe mode, this service doesn't detect any stations. Instead, it probes min/max/avg signal values in the provided frequency range, and optionally saves the whole spectrum to a csv file")
        public Builder probe() {
            probe = true;
            return this;
        }

        @BuilderParam(argName = "control-port", defaultField = "controlPort", description = "Control port for GNU Radio communication")
        public Builder setControlPort(int controlPort) {
            this.controlPort = controlPort;
            return this;
        }

        @BuilderParam(argName = "dc-filter-width", defaultField = "dcFilterWidth", description = "Width of the DC filter in HZ")
        public void setDcFilterWidth(float dcFilterWidth) {
            this.dcFilterWidth = dcFilterWidth;
        }

        @BuilderParam(argName = "end-freq", defaultField = "endFreq", description = "Target scan frequency")
        public Builder setEndFreq(float endFreq) {
            this.endFreq = endFreq;
            return this;
        }

        @BuilderParam(argName = "fft-size", defaultField = "fftSize", description = "FFT Size. Higher values increase scan accuracy, but require more processing power.")
        public Builder setFftSize(int fftSize) {
            this.fftSize = fftSize;
            return this;
        }

        @BuilderParam(argName = "probe-csv-file", description = "If probe mode is enabled, the whole spectrum will be saved to this file. If this option is ommitted, the spectrum will not be saved. \"-\" for stdin is NOT supported")
        public Builder setProbeCsvFile(String probeCsvFile) {
            this.probeCsvFile = probeCsvFile;
            return this;
        }

        @BuilderParam(argName = "rx-port", defaultField = "rxPort", description = "Receive port for FFT data")
        public void setRxPort(int rxPort) {
            this.rxPort = rxPort;
        }

        @BuilderParam(argName = "start-freq", defaultField = "startFreq", description = "Initial scan frequency")
        public Builder setStartFreq(float startFreq) {
            this.startFreq = startFreq;
            return this;
        }

        @BuilderParam(argName = "tune-delay", defaultField = "tuneDelay", description = "A delay between tuning to and probing the signal in milliseconds")
        public Builder setTuneDelay(int tuneDelay) {
            this.tuneDelay = tuneDelay;
            return this;
        }

    }

    private static final float SAMPLE_RATE = 24e5f;

    private final int controlPort;
    private final boolean dcBlock;
    private final float dcFilterWidth;
    private float[] fft;
    private final Object fftLock = new Object();
    private final int fftSize;
    private boolean requestFFT;
    private final int rxPort;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final float startFreq, endFreq;
    private final int tuneDelay;
    private Tuner tuner;
    private final boolean verbose;

    private FFTDiscoveryService(int fftSize, int controlPort, float endFreq, float startFreq, boolean verbose,
            int rxPort, int tuneDelay, boolean dcBlock, float dcFilterWidth) {
        this.dcFilterWidth = dcFilterWidth;
        this.fftSize = fftSize;
        this.controlPort = controlPort;
        this.startFreq = startFreq;
        this.endFreq = endFreq;
        this.verbose = verbose;
        this.rxPort = rxPort;
        this.tuneDelay = tuneDelay;
        fft = new float[fftSize];
        this.dcBlock = dcBlock;
    }

    @Override
    public List<RadioStation> decorate(List<RadioStation> decorate, ChainServiceDecorator decorator) throws Exception {
        return null;
    }

    @Override
    public List<RadioStation> discover() throws Exception {
        Process fftRx = GRScriptRunner.run("fft_rx.py", "-c", controlPort, "-t", fftSize);
        if (verbose) {
            System.err.println("Connecting with the receiver...");
        }
        try (RawMessageSender controller = new RawMessageSender("tcp://localhost:" + controlPort, false);
                Socket socket = service.submit(() -> {
                    while (true) {
                        try {
                            return new Socket("localhost", rxPort);
                        } catch (Exception e) {
                            Thread.sleep(500);
                        }
                    }
                }).get(10000, TimeUnit.MILLISECONDS);
                DataInputStream in = new DataInputStream(socket.getInputStream())) {
            service.submit(() -> {
                try {
                    while (fftRx.isAlive()) {
                        float[] read = readFFT(in, fftSize);
                        synchronized (fftLock) {
                            if (requestFFT) {
                                fft = read;
                                requestFFT = false;
                                fftLock.notify();
                            }
                        }
                    }
                } catch (Exception e) {}
            });
            tuner = DiscoveryService.createTuner(controller, false);
            controller.start();
            List<Float> points = new ArrayList<>();
            float lowerFreq = startFreq - SAMPLE_RATE / 2;
            float higherFreq = startFreq + SAMPLE_RATE / 2;
            for (float centerFreq = startFreq; centerFreq <= endFreq; centerFreq += SAMPLE_RATE) {
                tuner.tune(centerFreq);
                Thread.sleep(tuneDelay);
                if (verbose) System.err.println(String.format("Probed signal at %sHz (%sHz - %sHz)", centerFreq,
                        centerFreq - SAMPLE_RATE / 2, centerFreq + SAMPLE_RATE / 2));
                higherFreq = centerFreq + SAMPLE_RATE / 2;
                synchronized (fftLock) {
                    requestFFT = true;
                    fftLock.wait();
                }
                float max = Integer.MIN_VALUE;
                float freq = centerFreq;
                int point = 0;
                for (int i = 0; i < fft.length; i++) {
                    float f = fft[i];
                    points.add(f);
                    if (f > max) {
                        freq = calcRelativeFrequency(SAMPLE_RATE, i);
                        if (dcBlock && freq > -(dcFilterWidth / 2f) && freq < dcFilterWidth / 2f) continue;
                        max = f;
                        point = i;
                    }
                }
                if (verbose)
                    System.err.println(String.format("  Peak %sdB at point %s (%sHz)", max, point, centerFreq + freq));
            }

        } catch (TimeoutException ex) {
            throw new IOException("Couldn't connect with the receiver");
        } finally {
            service.shutdownNow();
            fftRx.destroyForcibly();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isDecoratingSupported() {
        return false;
    }

    private float calcRelativeFrequency(float bandwidth, int point) {
        return point / (float) (fftSize - 1) * bandwidth - bandwidth / 2f;
    }

    private static float[] readFFT(DataInputStream in, int fftSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        float[] fft = new float[fftSize];
        byte[] fData = new byte[4];
        for (int i = 0; i < fft.length; i++) {
            in.readFully(fData);
            buffer.position(0);
            buffer.put(fData);
            fft[i] = buffer.getFloat(0);
        }
        return fft;
    }

}
