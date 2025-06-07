package io.github.defective4.sdr.sdrdscv.service.decorator;

public interface ServiceDecoratorBuilder<T extends ServiceDecorator> {
    T build();
}
