package org.example.item01_static_factory_methods.bestpractice.dynamicreturn;

public class HighCompressionCompressor
        implements Compressor {

    @Override
    public byte[] compress(byte[] data) {

        System.out.println("High ratio compression");

        return data;
    }
}