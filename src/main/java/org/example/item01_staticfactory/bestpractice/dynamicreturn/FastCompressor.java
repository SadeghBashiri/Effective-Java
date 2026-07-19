package org.example.item01_staticfactory.bestpractice.dynamicreturn;

public class FastCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] data) {

        System.out.println("Fast compression");

        return data;
    }
}