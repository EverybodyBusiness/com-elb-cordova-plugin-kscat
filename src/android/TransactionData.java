package com.darryncampbell.cordova.plugin.intent;

public class TransactionData
{
    public byte[] dataLength = new byte[4];
    public byte[] transactionCode = new byte[2];
    public byte[] operationCode = new byte[2];
    public byte[] transferCode = new byte[4];
    public byte[] transferType = new byte[1];
    public byte[] deviceNumber = new byte[10];
    public byte[] companyInfo = new byte[4];
    public byte[] transferSerialNumber = new byte[12];
    public byte[] status = new byte[1];
    public byte[] standardCode = new byte[4];
    public byte[] cardCompanyCode = new byte[4];
    public byte[] transferDate = new byte[12];
    public byte[] cardType = new byte[1];
    public byte[] message1 = new byte[16];
    public byte[] message2 = new byte[16];
    public byte[] approvalNumber = new byte[12];
    public byte[] transactionUniqueNumber = new byte[20];

    public byte[] merchantNumber = new byte[15];
    public byte[] IssuanceCode = new byte[2];
    public byte[] cardCategoryName = new byte[16];
    public byte[] purchaseCompanyCode = new byte[2];
    public byte[] purchaseCompanyName = new byte[16];
    public byte[] workingKeyIndex = new byte[2];
    public byte[] workingKey = new byte[16];
    public byte[] balance = new byte[9];
    public byte[] point1 = new byte[9];
    public byte[] point2 = new byte[9];
    public byte[] point3 = new byte[9];
    public byte[] notice1 = new byte[20];
    public byte[] notice2 = new byte[40];
    public byte[] reserved = new byte[5];
    public byte[] KSNETreserved = new byte[40];
    public byte[] filler = new byte[30];
    public byte[] extraData = new byte[650];

    public void SetData(byte[] readData)
    {
        int totlen = readData.length;
        int readIdx = 1; // length 4, stx 1

        System.arraycopy(readData, 0, dataLength, 0, 4);
        readIdx += 4;
        System.arraycopy(readData, readIdx, transactionCode, 0, 2);
        readIdx += 2;
        System.arraycopy(readData, readIdx, operationCode, 0, 2);
        readIdx += 2;
        System.arraycopy(readData, readIdx, transferCode, 0, 4);
        readIdx += 4;
        System.arraycopy(readData, readIdx, transferType, 0, 1);
        readIdx += 1;
        System.arraycopy(readData, readIdx, deviceNumber, 0, 10);
        readIdx += 10;
        System.arraycopy(readData, readIdx, companyInfo, 0, 4);
        readIdx += 4;
        System.arraycopy(readData, readIdx, transferSerialNumber, 0, 12);
        readIdx += 12;
        System.arraycopy(readData, readIdx, status, 0, 1);
        readIdx += 1;
        System.arraycopy(readData, readIdx, standardCode, 0, 4);
        readIdx += 4;
        System.arraycopy(readData, readIdx, cardCompanyCode, 0, 4);
        readIdx += 4;
        System.arraycopy(readData, readIdx, transferDate, 0, 12);
        readIdx += 12;
        System.arraycopy(readData, readIdx, cardType, 0, 1);
        readIdx += 1;
        System.arraycopy(readData, readIdx, message1, 0, 16);
        readIdx += 16;
        System.arraycopy(readData, readIdx, message2, 0, 16);
        readIdx += 16;
        System.arraycopy(readData, readIdx, approvalNumber, 0, 12);
        readIdx += 12;
        System.arraycopy(readData, readIdx, transactionUniqueNumber, 0, 20);
        readIdx += 20;
        System.arraycopy(readData, readIdx, merchantNumber, 0, 15);
        readIdx += 15;
        System.arraycopy(readData, readIdx, IssuanceCode, 0, 2);            // 발급사 코드
        readIdx += 2;
        System.arraycopy(readData, readIdx, cardCategoryName, 0, 16);       // 카드종류 명
        readIdx += 16;
        System.arraycopy(readData, readIdx, purchaseCompanyCode, 0, 2);     // 매입사 코드
        readIdx += 2;
        System.arraycopy(readData, readIdx, purchaseCompanyName, 0, 16);    // 매입사 명
        readIdx += 16;
        System.arraycopy(readData, readIdx, workingKeyIndex, 0, 2);
        readIdx += 2;
        System.arraycopy(readData, readIdx, workingKey, 0, 16);
        readIdx += 16;
        System.arraycopy(readData, readIdx, balance, 0, 9);
        readIdx += 9;
        System.arraycopy(readData, readIdx, point1, 0, 9);
        readIdx += 9;
        System.arraycopy(readData, readIdx, point2, 0, 9);
        readIdx += 9;
        System.arraycopy(readData, readIdx, point3, 0, 9);
        readIdx += 9;
        System.arraycopy(readData, readIdx, notice1, 0, 20);
        readIdx += 20;
        System.arraycopy(readData, readIdx, notice2, 0, 40);
        readIdx += 40;
        System.arraycopy(readData, readIdx, reserved, 0, 5);
        readIdx += 5;
        System.arraycopy(readData, readIdx, KSNETreserved, 0, 40);
        readIdx += 40;
        System.arraycopy(readData, readIdx, filler, 0, 30);
        readIdx += 30;
        if(totlen > 458)    // 458넘으면 extra data 있다고 판단
            System.arraycopy(readData, readIdx, extraData, 0, totlen-readIdx);
    }
}
