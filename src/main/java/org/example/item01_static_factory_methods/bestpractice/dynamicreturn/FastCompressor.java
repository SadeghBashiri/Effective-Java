package org.example.item01_static_factory_methods.bestpractice.dynamicreturn;

public class FastCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] data) {

        System.out.println("Fast compression");

        return data;
    }
}