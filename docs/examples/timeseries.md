# Time-Series Examples

Working with time-series data and IoT sensors.

## Sensor Data Analysis

### Hourly Sensor Averages
```kotlin
data class HourlySensorData(
    val hour: String,
    val avgValue: Double,
    val minValue: Double,
    val maxValue: Double,
    val readingCount: Int
)

fun getHourlySensorData(sensorId: String, date: Date): List<HourlySensorData> {
    val dayStart = startOfDay(date)
    val dayEnd = endOfDay(date)
    
    return konduct.collection<SensorReading>()
        .match {
            SensorReading::sensorId eq sensorId
            SensorReading::timestamp gte dayStart
            SensorReading::timestamp lte dayEnd
        }
        .group {
            by(SensorReading::timestamp, unit = TimeUnit.HOUR)
            accumulate {
                "avgValue" avg SensorReading::value
                "minValue" min SensorReading::value
                "maxValue" max SensorReading::value
                "readingCount" count Unit
            }
        }
        .sort { "hour".asc() }
        .into<HourlySensorData>()
        .toList()
}
```

### Moving Average
```kotlin
data class SensorWithMovingAvg(
    val timestamp: Date,
    val value: Double,
    val movingAvg: Double
)

fun getSensorDataWithMovingAvg(sensorId: String): List<SensorWithMovingAvg> {
    return konduct.collection<SensorReading>()
        .match { SensorReading::sensorId eq sensorId }
        .sort { SensorReading::timestamp.asc() }
        .customStage("\$setWindowFields") {
            "sortBy" from { "timestamp" from 1 }
            "output" from {
                "movingAvg" from {
                    "\$avg" from "\$value"
                    "window" from {
                        "documents" from list(-5, 0)  // Last 5 readings + current
                    }
                }
            }
        }
        .into<SensorWithMovingAvg>()
        .toList()
}
```

## Anomaly Detection

### Detect Outliers
```kotlin
data class AnomalyReport(
    val timestamp: Date,
    val value: Double,
    val movingAvg: Double,
    val stdDev: Double,
    val deviation: Double,
    val isAnomaly: Boolean
)

fun detectAnomalies(sensorId: String, threshold: Double = 3.0): List<AnomalyReport> {
    return konduct.collection<SensorReading>()
        .match { SensorReading::sensorId eq sensorId }
        .sort { SensorReading::timestamp.asc() }
        .customStage("\$setWindowFields") {
            "sortBy" from { "timestamp" from 1 }
            "output" from {
                "movingAvg" from {
                    "\$avg" from "\$value"
                    "window" from {
                        "documents" from list(-10, -1)  // Previous 10 readings
                    }
                }
                "stdDev" from {
                    "\$stdDevPop" from "\$value"
                    "window" from {
                        "documents" from list(-10, -1)
                    }
                }
            }
        }
        .addFields {
            "deviation" from abs("\$value" - "\$movingAvg")
            "threshold" from ("\$stdDev" * threshold)
            "isAnomaly" from ("\$deviation" gte "\$threshold")
        }
        .match { "isAnomaly" eq true }
        .into<AnomalyReport>()
        .toList()
}
```

## Downsampling

### Aggregate to Lower Resolution
```kotlin
data class DailySummary(
    val date: String,
    val avgTemperature: Double,
    val minTemperature: Double,
    val maxTemperature: Double,
    val dataPoints: Int
)

fun downsampleToDaily(sensorId: String, days: Int): List<DailySummary> {
    return konduct.collection<TemperatureReading>()
        .match {
            TemperatureReading::sensorId eq sensorId
            TemperatureReading::timestamp gte daysAgo(days)
        }
        .group {
            by(TemperatureReading::timestamp, unit = TimeUnit.DAY)
            accumulate {
                "avgTemperature" avg TemperatureReading::value
                "minTemperature" min TemperatureReading::value
                "maxTemperature" max TemperatureReading::value
                "dataPoints" count Unit
            }
        }
        .sort { "date".asc() }
        .into<DailySummary>()
        .toList()
}
```

## Multi-Sensor Analysis

### Compare Multiple Sensors
```kotlin
data class SensorComparison(
    val hour: String,
    val sensor1Avg: Double,
    val sensor2Avg: Double,
    val difference: Double
)

fun compareSensors(sensor1Id: String, sensor2Id: String, date: Date): List<SensorComparison> {
    return konduct.collection<SensorReading>()
        .match {
            SensorReading::sensorId `in` listOf(sensor1Id, sensor2Id)
            SensorReading::timestamp gte startOfDay(date)
            SensorReading::timestamp lte endOfDay(date)
        }
        .group {
            by {
                "hour" from SensorReading::timestamp.hour()
                "sensorId" from SensorReading::sensorId
            }
            accumulate {
                "avgValue" avg SensorReading::value
            }
        }
        .group {
            by("hour")
            accumulate {
                "sensor1Avg" avgIf {
                    condition = { "sensorId" eq sensor1Id }
                    expression = "avgValue"
                }
                "sensor2Avg" avgIf {
                    condition = { "sensorId" eq sensor2Id }
                    expression = "avgValue"
                }
            }
        }
        .addFields {
            "difference" from ("\$sensor1Avg" - "\$sensor2Avg")
        }
        .sort { "hour".asc() }
        .into<SensorComparison>()
        .toList()
}
```

## Predictive Maintenance

### Equipment Health Score
```kotlin
data class EquipmentHealth(
    val equipmentId: String,
    val avgVibration: Double,
    val avgTemperature: Double,
    val peakVibration: Double,
    val healthScore: Double,
    val maintenanceNeeded: Boolean
)

fun calculateEquipmentHealth(): List<EquipmentHealth> {
    return konduct.collection<SensorReading>()
        .match {
            SensorReading::timestamp gte hoursAgo(24)
        }
        .group {
            by(SensorReading::equipmentId)
            accumulate {
                "avgVibration" avg SensorReading::vibration
                "avgTemperature" avg SensorReading::temperature
                "peakVibration" max SensorReading::vibration
            }
        }
        .addFields {
            "healthScore" from (
                100 -
                (("\$avgVibration" / 10) * 30) -  // Vibration impact: 30%
                (("\$avgTemperature" - 20) * 2) - // Temp impact: varies
                (("\$peakVibration" / 15) * 20)   // Peak impact: 20%
            )
            "maintenanceNeeded" from ("\$healthScore" lt 70)
        }
        .match { "maintenanceNeeded" eq true }
        .sort { "healthScore".asc() }
        .into<EquipmentHealth>()
        .toList()
}
```

## Energy Monitoring

### Peak Usage Detection
```kotlin
data class EnergyUsage(
    val hour: String,
    val avgConsumption: Double,
    val peakConsumption: Double,
    val isPeakHour: Boolean
)

fun analyzeEnergyUsage(days: Int = 7): List<EnergyUsage> {
    return konduct.collection<PowerReading>()
        .match {
            PowerReading::timestamp gte daysAgo(days)
        }
        .group {
            by(PowerReading::timestamp, unit = TimeUnit.HOUR)
            accumulate {
                "avgConsumption" avg PowerReading::watts
                "peakConsumption" max PowerReading::watts
            }
        }
        .addFields {
            "isPeakHour" from ("\$avgConsumption" gte 5000)
        }
        .sort { "avgConsumption".desc() }
        .into<EnergyUsage>()
        .toList()
}
```

### Cost Calculation
```kotlin
data class EnergyCost(
    val day: String,
    val peakHourKWh: Double,
    val offPeakKWh: Double,
    val peakCost: Double,
    val offPeakCost: Double,
    val totalCost: Double
)

fun calculateEnergyCost(month: Int, year: Int): List<EnergyCost> {
    val PEAK_RATE = 0.25
    val OFF_PEAK_RATE = 0.10
    
    return konduct.collection<PowerReading>()
        .match {
            PowerReading::timestamp.year() eq year
            PowerReading::timestamp.month() eq month
        }
        .addFields {
            "hour" from PowerReading::timestamp.hour()
            "isPeak" from ("\$hour" gte 9 and "\$hour" lte 21)
            "kWh" from (PowerReading::watts / 1000)
        }
        .group {
            by {
                "day" from PowerReading::timestamp.day()
                "isPeak" from "\$isPeak"
            }
            accumulate {
                "totalKWh" sum "\$kWh"
            }
        }
        .group {
            by("day")
            accumulate {
                "peakHourKWh" sumIf {
                    condition = { "isPeak" eq true }
                    expression = "totalKWh"
                }
                "offPeakKWh" sumIf {
                    condition = { "isPeak" eq false }
                    expression = "totalKWh"
                }
            }
        }
        .addFields {
            "peakCost" from ("\$peakHourKWh" * PEAK_RATE)
            "offPeakCost" from ("\$offPeakKWh" * OFF_PEAK_RATE)
            "totalCost" from ("\$peakCost" + "\$offPeakCost")
        }
        .sort { "day".asc() }
        .into<EnergyCost>()
        .toList()
}
```

## See Also

- [Grouping Guide](../guide/grouping.md) - Time-based aggregation
- [Custom Stages](../guide/custom-stages.md) - Window functions
- [Analytics Examples](analytics.md) - More analysis patterns