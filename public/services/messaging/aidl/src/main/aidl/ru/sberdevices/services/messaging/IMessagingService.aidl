package ru.sberdevices.services.messaging;

import ru.sberdevices.services.messaging.IMessagingListener;
import ru.sberdevices.services.messaging.model.MessageName;

interface IMessagingService {
    const int VERSION = 3;
    const String PLATFORM_VERSION = "1.83.0";

    /** @since version 1 */
    /** @deprecated use sendAction2() instead */
    String sendAction(in MessageName messageName, String payload) = 10;
    /** @since version 1 */
    /** @deprecated use sendActionWithAppID2() instead */
    String sendActionWithAppID(in MessageName messageName, String payload, String androidApplicationID) = 13;

    /** @since platform version 1.81.0 */
    /** @since version 2 */
    String sendAction2(in MessageName messageName, String payload, String stateLevel) = 11;
    /** @since platform version 1.81.0 */
    /** @since version 2 */
    String sendActionWithAppID2(in MessageName messageName, String payload, String androidApplicationID, String stateLevel) = 14;

    /** @since platform version 1.83.0 */
    /** @since version 3 */
    String sendAction3(in MessageName messageName, String payload, String stateLevel, String serverActionMode) = 12;
    /** @since platform version 1.83.0 */
    /** @since version 3 */
    String sendActionWithAppID3(in MessageName messageName, String payload, String androidApplicationID, String stateLevel, String serverActionMode) = 15;

    /** @since version 1 */
    void sendText(String text) = 20;

    /** @since version 1 */
    void addListener(in IMessagingListener listener) = 110;
    /** @since version 1 */
    void removeListener(in IMessagingListener listener) = 120;
}
