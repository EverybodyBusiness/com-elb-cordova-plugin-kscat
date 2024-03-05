package com.darryncampbell.cordova.plugin.intent;

public class ExtraReconnectKscat
{
    public byte[] dataLength = new byte[4];
    public byte[] transactionCode = new byte[2];
    public byte[] resultCode = new byte[2];


    public void SetData(byte[] readData)
    {
        int totlen = readData.length;
        int readIdx = 1; // length 4, stx 1

        System.arraycopy(readData, 0, dataLength, 0, 4);
        readIdx += 4;
        System.arraycopy(readData, readIdx, transactionCode, 0, 2);
        readIdx += 2;
        System.arraycopy(readData, readIdx, resultCode, 0, 1);
        readIdx += 1;
        System.arraycopy(readData, readIdx, filler, 0, 3);
//        readIdx += 3;
//        System.arraycopy(readData, readIdx, etx, 0, 1);
//        readIdx += 1;
//        System.arraycopy(readData, readIdx, CR, 0, 1);
    }
}
