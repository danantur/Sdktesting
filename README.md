# Sdktesting
Приложение, созданное с целью разобраться в работе официального SDK Microlife, который предоставляет алгоритмы, реализующие протоколы Bluetooth устройств Microlife
# microlife-sdk
Декомпилированные .aar библиотеки, соединённые в одном модуле, который также можно скомпилировать в .aar библиотеку (изначально sdk использует два .aar файла и две второстепенные библиотеки)
# Подключение и обмен данными с термометром
Каждый раз, после подключения к термометру, он отправляет все последние замеры, которые им проводились, при этом, если телефон подключен к термометру и тот выполняет замер, то SDK отключается от термометра, когда это происходит, нужно снова к нему подключиться и получить только что созданный замер (также при подключении термометр отправляет свои данные через делегат onResponseDeviceInfo)
# Подключение и обмен данными с измерителем давления
Чтобы подключиться к нему, требуется проверить тип устройства (если в начале названия стоит символ "A", то нужно просто подключиться, иначе, нужно связать телефон и BPM через bond (пока что не реализовано)), после подключения для получения данных нужно отправлять комманды на BPM и фиксировать данные в соответствующих делегатах.

Если было выполнено подключение (в случае с типом устройства, где требуется bond, то BPMProtocol.ConnectState.Connected ещё не говорит о подключении к устройству), то лучше вызвать метод disconnectBPM()