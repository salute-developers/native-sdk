package ru.sberdevices.services.appstate;

import ru.sberdevices.services.appstate.IAppStateProvider;
import ru.sberdevices.services.appstate.IAppStateStatusListener;

interface IAppStateService {
   const int VERSION = 2;

   /** @since version 1 */
   void setProvider(@nullable IAppStateProvider provider) = 110;
   /** @since version 2 */
   void setProviderForApp(@nullable IAppStateProvider provider, String packageName) = 111;

   /** @since version 2 */
   void addAppStateStatusListener(in IAppStateStatusListener listener) = 120;
   /** @since version 2 */
   void removeAppStateStatusListener(in IAppStateStatusListener listener) = 121;

   /** @since version 2 */
   void registerBackgroundApp(String packageName) = 130;
   /** @since version 2 */
   void unregisterBackgroundApp(String packageName) = 131;
}
