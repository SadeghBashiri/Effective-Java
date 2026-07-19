package org.example.item01_staticfactory.bestpractice.dynamicreturn;

public final class CompressorFactory {

    private CompressorFactory() {
    }

    public static Compressor create(byte[] data) {

        if (data.length < 1024) {
            return new FastCompressor();
        }

        return new HighCompressionCompressor();
    }
}