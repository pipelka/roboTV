package org.xvdr.extractor;

public interface BufferPacket {

    byte[] data();

    void rewind();

    char getU8();

    byte getS8();

    int getU16();

    short getS16();

    long getU32();

    int getS32();

    long getU64();

    long getS64();

    String getString();

    void readBuffer(byte[] buffer, int offset, int length);
}
