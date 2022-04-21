package ru.sberdevices.services.paylib.codes;

/**
 * Общий протокол кодирования и декодирования int-значений, передаваемых по биндеру.
 * Используется в [IPayStatusListener::onPayStatusUpdated].
 */
public enum PayResultCode {
    SUCCESS(0),
    ERROR(1),
    CANCELLED(2);

    public final int rawCode;

    private PayResultCode(int rawCode) {
        this.rawCode = rawCode;
    }
}
