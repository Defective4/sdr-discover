package io.github.defective4.sdr.sdrdscv.service.decorator.impl;

import java.util.List;

import io.github.defective4.sdr.sdrdscv.annotation.BuilderParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecoratorBuilder;

public class ChainServiceDecorator implements ServiceDecorator {

    public static class Builder implements ServiceDecoratorBuilder<ChainServiceDecorator> {

        private boolean replaceMeta, ignoreMeta;
        private boolean replaceModulation = true;
        private boolean replaceNames = true;

        @Override
        public ChainServiceDecorator build() {
            return new ChainServiceDecorator(replaceMeta, ignoreMeta, replaceNames, replaceModulation);
        }

        @BuilderParam(argName = "ignore-meta", description = "Keep all previous metadata intact.")
        public void ignoreMeta() {
            ignoreMeta = true;
        }

        @BuilderParam(argName = "keep-modulation", description = "Doen't replace modulation with this decorator's results.")
        public void keepModulation() {
            replaceModulation = false;
        }

        @BuilderParam(argName = "keep-names", description = "If enabled, this decorator won't replace station names.")
        public Builder keepNames() {
            replaceNames = false;
            return this;
        }

        @BuilderParam(argName = "replace-meta", description = "If enabled, all metadata will be replaced by the decorator, instead of merging.")
        public Builder replaceMeta() {
            replaceMeta = true;
            return this;
        }

    }

    private final boolean replaceMeta, ignoreMeta, replaceNames, replaceModulation;

    private ChainServiceDecorator(boolean replaceMeta, boolean ignoreMeta, boolean replaceNames,
            boolean replaceModulation) {
        this.replaceMeta = replaceMeta;
        this.ignoreMeta = ignoreMeta;
        this.replaceNames = replaceNames;
        this.replaceModulation = replaceModulation;
    }

    @Override
    public List<RadioStation> decorate(List<RadioStation> stations) {
        return stations;
    }

    public boolean isIgnoreMeta() {
        return ignoreMeta;
    }

    public boolean isReplaceMeta() {
        return replaceMeta;
    }

    public boolean isReplaceModulation() {
        return replaceModulation;
    }

    public boolean isReplaceNames() {
        return replaceNames;
    }

}
