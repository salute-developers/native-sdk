package ru.sberdevices.services.paylib;

import ru.sberdevices.services.paylib.IPayStatusListener;

interface IPayLibService {
    const String PLATFORM_VERSION = "1.79.0";
    const int VERSION = 1;

    /** @since version 1;*/
    boolean launchPayDialog(in String invoiceId) = 10;
    /** @since version 1;*/
    void addPayStatusListener(in IPayStatusListener listener) = 120;
    /** @since version 1;*/
    void removePayStatusListener(in IPayStatusListener listener) = 121;
}
