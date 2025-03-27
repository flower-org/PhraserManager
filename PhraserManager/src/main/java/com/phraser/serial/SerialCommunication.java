package com.phraser.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortIOException;
import com.phraser.utils.UnsignedConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.Adler32;

import static com.google.common.base.Preconditions.checkNotNull;

public class SerialCommunication {
    final static Logger LOGGER = LoggerFactory.getLogger(SerialCommunication.class);

    public static final byte[] MAGIC_NUMBER = {(byte) 0xC3, (byte) 0xD2, (byte) 0xE1, (byte) 0xF0};

    enum Operation {
        HELLO((short)1),
        BYE((short)2),
        START_BACKUP((short)3),
        START_BACKUP_CONFIRMATION((short)4),
        START_RESTORE((short)5),
        START_RESTORE_CONFIRMATION((short)6),
        BACKUP_BLOCK((short)7),
        BACKUP_BLOCK_RECEIVED((short)8),
        RESTORE_BLOCK((short)9),
        RESTORE_BLOCK_RECEIVED((short)10);

        public final short opCode;
        Operation(short opCode) {
            this.opCode = opCode;
        }

        static Operation of(short code) {
            switch (code) {
                case 1: return HELLO;
                case 2: return BYE;
                case 3: return START_BACKUP;
                case 4: return START_BACKUP_CONFIRMATION;
                case 5: return START_RESTORE;
                case 6: return START_RESTORE_CONFIRMATION;
                case 7: return BACKUP_BLOCK;
                case 8: return BACKUP_BLOCK_RECEIVED;
                case 9: return RESTORE_BLOCK;
                case 10: return RESTORE_BLOCK_RECEIVED;
                default: throw new RuntimeException("Unknown Operation Code " + code);
            }
        }
    }

    static class Message {
        final Operation operation;
        @Nullable final Short bank;
        @Nullable final Integer blockNumber;
        @Nullable final byte[] data;
        @Nullable final Long adler32;

        Message(Operation operation) {
            this(operation, null, null);
        }

        Message(Operation operation, @Nullable Short bank, @Nullable Integer blockNumber) {
            this(operation, bank, blockNumber, null, null);
        }

        Message(Operation operation,
                @Nullable Short bank,
                @Nullable Integer blockNumber,
                @Nullable byte[] data,
                @Nullable Long adler32) {
            this.operation = operation;
            this.bank = bank;
            this.blockNumber = blockNumber;
            this.data = data;
            this.adler32 = adler32;
        }
    }

    public static int byteArrayToInt(byte[] byteArray) {
        if (byteArray.length != 4) {
            throw new IllegalArgumentException("Byte array must be exactly 4 bytes long.");
        }

        return ((byteArray[0] & 0xFF) << 24) |
                ((byteArray[1] & 0xFF) << 16) |
                ((byteArray[2] & 0xFF) << 8) |
                (byteArray[3] & 0xFF);
    }

    public static byte[] intToByteArray(int value) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) ((value >> 24) & 0xFF); // Most significant byte
        byteArray[1] = (byte) ((value >> 16) & 0xFF);
        byteArray[2] = (byte) ((value >> 8) & 0xFF);
        byteArray[3] = (byte) (value & 0xFF); // Least significant byte
        return byteArray;
    }

    static Message readMessage(SerialPort serialPort) throws IOException {
        // 1.1 Read Operation Code
        byte[] opCode = serialPort.getInputStream().readNBytes(1);
        Operation operation = Operation.of(UnsignedConverter.byteToShort(opCode[0]));
        if (operation == Operation.HELLO || operation == Operation.BYE
                || operation == Operation.START_BACKUP
                || operation == Operation.START_RESTORE) {
            return new Message(operation);
        }

        // 1.2 Block Number
        byte[] bankBytes = serialPort.getInputStream().readNBytes(1);
        short bank = UnsignedConverter.byteToShort(bankBytes[0]);
        byte[] blockNumberBytes = serialPort.getInputStream().readNBytes(4);
        int blockNumber = byteArrayToInt(blockNumberBytes);
        if (operation == Operation.BACKUP_BLOCK_RECEIVED || operation == Operation.RESTORE_BLOCK_RECEIVED
                || operation == Operation.START_BACKUP_CONFIRMATION || operation == Operation.START_RESTORE_CONFIRMATION) {
            return new Message(operation, bank, blockNumber);
        }

        // 1.3 Length
        byte[] lengthBytes = serialPort.getInputStream().readNBytes(4);
        int length = byteArrayToInt(lengthBytes);

        // 1.4 Data
        byte[] dataBytes = serialPort.getInputStream().readNBytes(length);

        // 1.5 Adler32
        byte[] adler32Bytes = serialPort.getInputStream().readNBytes(4);
        int adler32Int = byteArrayToInt(adler32Bytes);
        long adler32 = UnsignedConverter.intToLong(adler32Int);

        return new Message(operation, bank, blockNumber, dataBytes, adler32);
    }

    static void sendMessage(SerialPort serialPort, Message message) throws IOException {
        serialPort.getOutputStream().write(MAGIC_NUMBER);

        // 1.1 Send Operation Code
        Operation operation = message.operation;
        serialPort.getOutputStream().write(UnsignedConverter.shortToByte(operation.opCode));
        if (operation == Operation.HELLO || operation == Operation.BYE
                || operation == Operation.START_BACKUP
                || operation == Operation.START_RESTORE) {
            return;
        }

        // 1.2.1 Bank #
        // 1.2.2 Block Number
        serialPort.getOutputStream().write(UnsignedConverter.shortToByte(checkNotNull(message.bank)));
        serialPort.getOutputStream().write(intToByteArray(checkNotNull(message.blockNumber)));
        if (operation == Operation.BACKUP_BLOCK_RECEIVED || operation == Operation.RESTORE_BLOCK_RECEIVED
            || operation == Operation.START_BACKUP_CONFIRMATION || operation == Operation.START_RESTORE_CONFIRMATION) {
            return;
        }

        // 1.3 Length
        byte[] lengthBytes = intToByteArray(checkNotNull(message.data).length);
        serialPort.getOutputStream().write(lengthBytes);

        // 1.4 Data
        serialPort.getOutputStream().write(checkNotNull(message.data));

        // 1.5 Adler32
        int unsignedAdler32 = UnsignedConverter.longToInt(checkNotNull(message.adler32));
        serialPort.getOutputStream().write(intToByteArray(unsignedAdler32));
    }

    enum ProtocolStage {
        HANDSHAKE,
        NEGOTIATE_OPERATION,
        BACKUP,
        RESTORE
    }

    public static void main(String[] args) throws Exception {
        File loadDb = new File("/home/john/test.phr");

        File saveDb = new File("/home/john/new.phr");
        int backupBlockCount = 128;
        runSequence("/dev/ttyACM0", (short)1, saveDb, loadDb, backupBlockCount, LOGGER::info);
    }

    // TODO: fix this
    public static volatile @Nullable SerialPort serialPort;
    public static void runSequence(String comPort, short bank, @Nullable File saveDb, @Nullable File loadDb,
                                   @Nullable Integer backupBlockCount, Consumer<String> logger) throws Exception {
        serialPort = SerialPort.getCommPort(comPort); // Change to your port

        serialPort.setComPortParameters(115200, 8, 1, 0); // Match the baud rate to your RP2040
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

        int restoreBlockCount = 128; //dummy value

        RandomAccessFile loadDbRaf = null;
        if (loadDb != null) {
            loadDbRaf = new RandomAccessFile(loadDb, "r");
        }
        Supplier<RandomAccessFile> saveDbRafSupplier = new Supplier<>() {
            @Nullable RandomAccessFile saveDbRaf = null;
            @Override
            public @Nullable RandomAccessFile get() {
                try {
                    if (saveDbRaf == null) {
                        if (saveDb != null) {
                            if (saveDb.exists()) {
                                saveDb.delete();
                            }
                            saveDb.createNewFile();
                            saveDbRaf = new RandomAccessFile(saveDb, "rwd");
                        }
                    }
                    return saveDbRaf;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        if (serialPort.openPort()) {
            try {
                logger.accept("Port opened successfully.");

                int blockNumber = 0;
                ProtocolStage stage = ProtocolStage.HANDSHAKE;
                boolean magicNumberCaught = false;
                // Continuously read from the serial port
                while (true) {
                    SerialPort serialPort = SerialCommunication.serialPort;
                    if (serialPort == null) {
                        return;
                    }

                    // Check if data is available to read
                    if (serialPort.bytesAvailable() > 0) {
                        if (!magicNumberCaught) {
                            byte[] b1 = new byte[1];
                            if (serialPort.getInputStream().read(b1) > 0) {
                                magicNumberCaught = catchMagicNumber(b1);
                            }
                        } else {
                            magicNumberCaught = false;
                            Message message = readMessage(serialPort);
                            if (stage == ProtocolStage.HANDSHAKE) {
                                if (message.operation != Operation.HELLO) {
                                    throw new RuntimeException("Unexpected opCode at HANDSHAKE: " + message.operation);
                                } else {
                                    logger.accept("HELLO received, sending HELLO back");
                                    sendMessage(serialPort, new Message(Operation.HELLO));
                                    stage = ProtocolStage.NEGOTIATE_OPERATION;
                                }
                            } else if (stage == ProtocolStage.NEGOTIATE_OPERATION) {
                                if (message.operation == Operation.HELLO) {
                                    //Client is spamming hellos, so this should be ignored
//                                logger.accept("Parasitic HELLO received, ignoring");
                                } else if (message.operation == Operation.START_BACKUP) {
                                    if (saveDbRafSupplier.get() == null) {
                                        throw new RuntimeException("Phraser device is requesting START_BACKUP, but restore file is not specified");
                                    }
                                    if (backupBlockCount == null) {
                                        throw new RuntimeException("Phraser device is requesting START_BACKUP, but Block Count is not specified");
                                    }

                                    logger.accept("START_BACKUP received, sending START_BACKUP_CONFIRMATION back");
                                    sendMessage(serialPort, new Message(Operation.START_BACKUP_CONFIRMATION, bank, backupBlockCount));
                                    logger.accept("START_BACKUP_CONFIRMATION sent; backupBlockCount" + backupBlockCount);
                                    stage = ProtocolStage.BACKUP;
                                } else if (message.operation == Operation.START_RESTORE) {
                                    if (loadDbRaf == null) {
                                        throw new RuntimeException("Phraser device is requesting START_RESTORE, but restore file is not specified");
                                    }

                                    restoreBlockCount = (int) ((loadDbRaf).length() / 4096L);
                                    logger.accept("START_RESTORE received, sending START_RESTORE_CONFIRMATION back; restoreBlockCount " + restoreBlockCount);
                                    sendMessage(serialPort, new Message(Operation.START_RESTORE_CONFIRMATION, bank, restoreBlockCount));

                                    // send RESTORE_BLOCK firstBlockNumber (0)
                                    logger.accept("sending RESTORE_BLOCK 0");
                                    byte[] blockData = loadBlock(blockNumber, loadDbRaf);

                                    Adler32 adler32 = new Adler32();
                                    adler32.update(blockData, 0, blockData.length);
                                    long checksum = adler32.getValue();
                                    sendMessage(serialPort, new Message(Operation.RESTORE_BLOCK, bank, 0, blockData, checksum));

                                    logger.accept("RESTORE_BLOCK 0 sent");

                                    stage = ProtocolStage.RESTORE;
                                } else {
                                    throw new RuntimeException("Unexpected opCode at NEGOTIATE_OPERATION: " + message.operation);
                                }
                            } else if (stage == ProtocolStage.BACKUP) {
                                if (message.operation == Operation.BYE) {
                                    if (backupBlockCount == null || blockNumber != backupBlockCount) {
                                        throw new RuntimeException("BACKUP_BLOCK final block count mismatch " + blockNumber + " : " + backupBlockCount);
                                    }
                                    logger.accept("BYE received, sending BYE back");
                                    sendMessage(serialPort, new Message(Operation.BYE));
                                    // Sleep here or Thumby might get BYE message corrupted (not sure why)
                                    Thread.sleep(1000);
                                    serialPort.flushIOBuffers();
                                    logger.accept("Quitting");

                                    serialPort.closePort();
                                    serialPort = null;
                                    return;
                                }
                                if (message.operation == Operation.BACKUP_BLOCK) {
                                    logger.accept("BACKUP_BLOCK received " + message.blockNumber);
                                    if (checkNotNull(message.blockNumber) != blockNumber) {
                                        throw new RuntimeException("BACKUP_BLOCK block number mismatch " + blockNumber + " : " + message.blockNumber);
                                    }
                                    blockNumber++;

                                    if (checkNotNull(message.data).length != 4096) {
                                        throw new RuntimeException("Block size != 4096; " + message.data.length);
                                    }
                                    Adler32 adler32 = new Adler32();
                                    adler32.update(message.data, 0, message.data.length);
                                    long checksum = adler32.getValue();
                                    if (checksum != checkNotNull(message.adler32)) {
                                        throw new RuntimeException("Checksum mismatch " + checksum + " : " + message.adler32);
                                    }

                                    saveBlock(message.blockNumber, message.data, saveDbRafSupplier.get());
                                    logger.accept("BACKUP_BLOCK saved, sending BACKUP_BLOCK_RECEIVED back");

                                    sendMessage(serialPort, new Message(Operation.BACKUP_BLOCK_RECEIVED, bank, message.blockNumber));
                                    logger.accept("BACKUP_BLOCK_RECEIVED sent");
                                } else {
                                    throw new RuntimeException("Unexpected opCode at BACKUP: " + message.operation);
                                }
                            } else if (stage == ProtocolStage.RESTORE) {
                                if (message.operation == Operation.BYE) {
                                    logger.accept("BYE received, quitting");

                                    serialPort.closePort();
                                    serialPort = null;
                                    return;
                                }
                                if (message.operation == Operation.RESTORE_BLOCK_RECEIVED) {
                                    logger.accept("RESTORE_BLOCK_RECEIVED message received " + message.blockNumber);
                                    if (checkNotNull(message.blockNumber) != blockNumber) {
                                        throw new RuntimeException("RESTORE_BLOCK_RECEIVED block number mismatch " + blockNumber + " : " + message.blockNumber);
                                    }
                                    blockNumber++;

                                    if (blockNumber == restoreBlockCount) {
                                        logger.accept("All blocks sent, sending BYE");
                                        sendMessage(serialPort, new Message(Operation.BYE));
                                    } else {
                                        //send next block RESTORE_BLOCK or END
                                        byte[] blockData = loadBlock(blockNumber, checkNotNull(loadDbRaf));

                                        Adler32 adler32 = new Adler32();
                                        adler32.update(blockData, 0, blockData.length);
                                        long checksum = adler32.getValue();
                                        sendMessage(serialPort, new Message(Operation.RESTORE_BLOCK, bank, blockNumber, blockData, checksum));

                                        logger.accept("RESTORE_BLOCK sent " + blockNumber);
                                    }
                                } else {
                                    throw new RuntimeException("Unexpected opCode at RESTORE: " + message.operation);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (serialPort != null) {
                    serialPort.closePort();
                    serialPort = null;
                }
                throw e;
            }
        } else {
            if (serialPort != null) {
                RuntimeException exc = new RuntimeException("Failed to open the port. Error: " + serialPort.getLastErrorCode() +
                        " / " + serialPort.getLastErrorLocation());
                serialPort = null;
                throw exc;
            } else {
                throw new RuntimeException("Failed to open the port");
            }
        }
    }

    private static void saveBlock(Integer blockNumber, byte[] data, RandomAccessFile saveDbRaf) throws IOException {
        saveDbRaf.seek(4096L * blockNumber);
        saveDbRaf.write(data);
    }

    private static byte[] loadBlock(int blockNumber, RandomAccessFile loadDbRaf) throws IOException {
        byte[] data = new byte[4096];
        loadDbRaf.seek(4096L * blockNumber);
        loadDbRaf.read(data);
        return data;
    }

    public static byte[] last4 = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    public static boolean catchMagicNumber(byte[] b1) {
        last4[0] = last4[1];
        last4[1] = last4[2];
        last4[2] = last4[3];
        last4[3] = b1[0];

        boolean isMagicNumber = Arrays.equals(MAGIC_NUMBER, last4);
        if (isMagicNumber) {
            last4[0] = 0;
            last4[1] = 0;
            last4[2] = 0;
            last4[3] = 0;
        }

        return isMagicNumber;
    }
}
