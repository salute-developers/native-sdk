package ru.sberdevices.services.paylib;

interface IPayStatusListener {
   oneway void onPayStatusUpdated(in String invoiceId, in int resultCode) = 10;
}
