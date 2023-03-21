package ru.sberdevices.services.published.environment.info.callbacks;

interface IVersionCallback {
    oneway void onVersionResult(in String version) = 10;
}
