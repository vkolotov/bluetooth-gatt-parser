[![Maven Central](https://img.shields.io/maven-central/v/org.openhab/bluetooth-gatt-parser.svg)](https://mvnrepository.com/artifact/org.openhab/bluetooth-gatt-parser)

A **simple** library/framework to work with Bluetooth Smart (BLE) GATT services and characteristics.

> This is a fork of the no longer maintained project at
> https://github.com/sputnikdev/bluetooth-gatt-parser.

Parsing a standard characteristic (Battery Level, `0x2A19`) is a one-liner:

```java
BluetoothGattParserFactory.getDefault().parse("2A19", new byte[] {51}).get("Level").getInteger(null);
```

This prints `51`.

## Features

1. Ships the standard [Bluetooth SIG GATT services and characteristics](https://www.bluetooth.com/specifications/assigned-numbers/), plus a number
   of characteristics recovered from the SIG's retired characteristic XML and
   generated from the current [GATT Specification Supplement](https://www.bluetooth.com/specifications/gss/).
2. Parses single- and multi-field characteristics into a user-friendly data format.
3. Serializes (writes) single- and multi-field characteristics.
4. Validates input against the GATT specification (format types and mandatory fields).
5. Supports variable-length array fields (e.g. the RR-Interval list in Heart Rate Measurement).
6. Extensible: user-defined services and characteristics via drop-in XML.
7. Supports all defined [format types](https://www.bluetooth.com/specifications/assigned-numbers/), including the
   IEEE-11073 `SFLOAT`/`FLOAT` (GSS `medfloat16`/`medfloat32`) types.

## Usage

Add the Maven dependency:

```xml
<dependency>
  <groupId>org.openhab</groupId>
  <artifactId>bluetooth-gatt-parser</artifactId>
  <version>X.Y.Z</version>
</dependency>
```

Reading and writing multi-field characteristics:

```java
// A default parser that reads/writes the bundled standard GATT services and characteristics.
BluetoothGattParser parser = BluetoothGattParserFactory.getDefault();

// Read Body Sensor Location (0x2A38) — a single-field characteristic.
GattResponse response = parser.parse("2A38", new byte[] {1}); // 1 == Chest
int sensorLocation = response.get("Body Sensor Location").getInteger(null); // 1 (Chest)

// Read Heart Rate Measurement (0x2A37) — a multi-field characteristic.
response = parser.parse("2A37", new byte[] {20, 74, 13, 3});
int heartRate = response.get("Heart Rate Measurement Value (8 bit resolution)").getInteger(null); // 74

// Write Heart Rate Control Point (0x2A39).
GattRequest request = parser.prepare("2A39");
request.setField("Heart Rate Control Point", 1);
byte[] data = parser.serialize(request);
```

See more in the integration tests:
[GenericCharacteristicParserIntegrationTest](src/test/java/org/openhab/bluetooth/gattparser/GenericCharacteristicParserIntegrationTest.java).

## Extending with user-defined services and characteristics

The library can add support for custom services/characteristics, or override a
bundled one, purely by providing a GATT XML file — no code required. See the
bundled [Battery Level characteristic](src/main/resources/gatt/characteristic/org.bluetooth.characteristic.battery_level.xml)
for the schema.

```java
BluetoothGattParser parser = BluetoothGattParserFactory.getDefault();
parser.loadExtensionsFromFolder(new File("/path/to/gatt-extensions"));
```

If the generic parser is not enough for a given characteristic, register your own:

```java
BluetoothGattParser parser = BluetoothGattParserFactory.getDefault();
parser.registerParser(CHARACTERISTIC_UUID, myCustomParser);
```

## Bundled GATT specifications

Each characteristic/service is one XML file under `src/main/resources/gatt/`. A
build-time generator indexes them into `gatt_spec_registry.json` (the `type`
attribute of each file must equal its filename).

The Bluetooth SIG **retired** the machine-readable characteristic XML repository
around 2019. Since then it publishes the [GATT Specification Supplement (GSS)](https://www.bluetooth.com/specifications/gss/)
as YAML, where the field structure is present but scaling/unit/presence are
encoded as a rigid mini-grammar inside prose. The characteristics added here were
derived from that GSS: scalars are high fidelity, while cases the prose cannot
express unambiguously carry an inline `<!-- REVIEW -->` marker rather than a
fabricated value.

## Contribution

Contributions are welcome. Build with Maven:

```bash
mvn clean install
```

Cut a release to Maven Central:

```bash
mvn release:prepare -B
mvn release:perform
```
