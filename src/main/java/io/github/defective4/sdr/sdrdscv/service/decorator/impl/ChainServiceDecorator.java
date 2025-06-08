package io.github.defective4.sdr.sdrdscv.service.decorator.impl;

import java.util.List;

import io.github.defective4.sdr.sdrdscv.annotation.BuilderParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecoratorBuilder;

public class ChainServiceDecorator implements ServiceDecorator {

    public static class Builder implements ServiceDecoratorBuilder<ChainServiceDecorator> {

        private boolean replaceMeta, ignoreMeta;
        private boolean replaceNames = true;

        @Override
        public ChainServiceDecorator build() {
            return new ChainServiceDecorator(replaceMeta, ignoreMeta, replaceNames);
        }

        @BuilderParam(argName = "ignore-meta", description = "Keep all previous metadata intact.")
        public void ignoreMeta() {
            ignoreMeta = true;
        }

        @BuilderParam(argName = "replace-meta", description = "If enabled, all metadata will be replaced by the decorator, instead of merging.")
        public Builder replaceMeta() {
            replaceMeta = true;
            return this;
        }

        @BuilderParam(argName = "keep-names", description = "If enabled, this decorator won't replace station names.")
        public Builder replaceNames() {
            replaceNames = true;
            return this;
        }

    }

    private final boolean replaceMeta, ignoreMeta, replaceNames;

    private ChainServiceDecorator(boolean replaceMeta, boolean ignoreMeta, boolean replaceNames) {
        this.replaceMeta = replaceMeta;
        this.ignoreMeta = ignoreMeta;
        this.replaceNames = replaceNames;
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

    public boolean isReplaceNames() {
        return replaceNames;
    }

}
