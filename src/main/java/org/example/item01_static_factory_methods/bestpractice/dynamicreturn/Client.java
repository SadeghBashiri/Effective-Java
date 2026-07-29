package org.example.item01_static_factory_methods.bestpractice.dynamicreturn;

public class Client {

    public static void main(String[] args) {

        byte[] data = new byte[5000];

        Compressor compressor =
                CompressorFactory.create(data);

        compressor.compress(data);
    }
}