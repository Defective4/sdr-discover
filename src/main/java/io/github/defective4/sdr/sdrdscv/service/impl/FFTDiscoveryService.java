package io.github.defective4.sdr.sdrdscv.service.impl;

import java.io.IOException;
import java.net.Socket;
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
        private float endFreq = 108e6f;
        private int fftSize = 1024;
        private boolean probe = false;
        private String probeCsvFile = null;
        private int rxPort = 2000;
        private float startFreq = 88e6f;
        private int tuneDelay = 100;

        @Override
        public FFTDiscoveryService build() throws Exception {
            return new FFTDiscoveryService(fftSize, controlPort, endFreq, startFreq, verbose, rxPort);
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

    private final int controlPort;
    private final int fftSize;
    private final int rxPort;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final float startFreq, endFreq;
    private Tuner tuner;
    private final boolean verbose;

    private FFTDiscoveryService(int fftSize, int controlPort, float endFreq, float startFreq, boolean verbose,
            int rxPort) {
        this.fftSize = fftSize;
        this.controlPort = controlPort;
        this.startFreq = startFreq;
        this.endFreq = endFreq;
        this.verbose = verbose;
        this.rxPort = rxPort;
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
                }).get(10000, TimeUnit.MILLISECONDS)) {
            tuner = DiscoveryService.createTuner(controller, false);
            controller.start();

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

}
