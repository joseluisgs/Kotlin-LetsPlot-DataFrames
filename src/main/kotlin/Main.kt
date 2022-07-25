import models.accidentes.*
import mu.KotlinLogging
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.jetbrains.kotlinx.dataframe.io.writeJson
import java.io.File

private val logger = KotlinLogging.logger {}

// https://kotlin.github.io/dataframe/overview.html

fun main() {
    println("Hello, Let's Plot!")
    ejemploAccidentes()

}

private fun ejemploAccidentes() {
    println("Ejemplo de Accidentes")
    println("======================")
    // Vamos a leer los accidentes
    val accidentes by lazy { loadAccidentesFromCsv(File("data/accidentes.csv")) }

    // cargamos el DataFrame original
    val df by lazy { accidentes.toDataFrame() }

    //Mostramos el esquema e imprimimos las 5 primeras filas
    println("Esquema del DataFrame")
    println(df.schema())
    println(df.head(5))
    println("Numero de filas: ${df.rowsCount()}")
    df.select("numExpediente").print(10)

    // Vamos a agrupar accidentes por mes
    val porMes = df.convert { fecha }.with { it.month }
        .groupBy { fecha }
        .aggregate {
            count() into "numAccidentes"
        }

    // Imprimimos el resultado
    println("Accidentes por mes:")
    println(porMes)

    // Media de accidentes por mes
    val mediaAccidentesPorMes = porMes.select("numAccidentes").mean()
    println("Media de accidentes por mes: $mediaAccidentesPorMes")

    // Estadisticas por mes hombres y mujeres
    val porMesSexo = df.convert { fecha }.with { it.month }
        .groupBy { fecha }
        .aggregate {
            count() into "numAccidentes"
            count { it.sexo == "Hombre" } into "numAccidentesHombres"
            count { it.sexo == "Mujer" } into "numAccidentesMujeres"
        }
    println("Accidentes por mes hombres y mujeres: ")
    println(porMesSexo)
    // Salvamos
    porMesSexo.writeCSV(File("reports/accidentesPorMesSexo.csv"))

    // Estadisticas por distritos
    val porDistrito = df.groupBy { distrito }
        .aggregate {
            count() into "numAccidentes"
            count { it.sexo == "Hombre" } into "numAccidentesHombres"
            count { it.sexo == "Mujer" } into "numAccidentesMujeres"
        }
    println("Accidentes por distrito:")
    println(porDistrito)
    porDistrito.writeCSV(File("reports/accidentesPorDistritoSexo.csv"))

    // Estadisticas por meses con alcohol y drogas
    val porMesesAlcoholDrogas = df.convert { fecha }.with { it.month }
        .groupBy { fecha }
        .aggregate {
            count() into "numAccidentes"
            count { it.positivoAlcohol } into "numAccidentesAlcohol"
            count { it.positivoDrogas } into "numAccidentesDrogas"
            count { it.positivoAlcohol && it.positivoDrogas } into "numAccidentesAlcoholDrogas"
        }

    println("Accidentes por meses con alcohol y drogas:")
    println(porMesesAlcoholDrogas)
    porMesesAlcoholDrogas.writeJson(File("reports/accidentesPorMesesAlcoholDrogas.json"))

    // Estadisticas alcohol y drogas po distrito
    val porDistritoAlcoholDrogas = df.groupBy { distrito }
        .aggregate {
            count() into "numAccidentes"
            count { it.positivoAlcohol } into "numAccidentesAlcohol"
            count { it.positivoDrogas } into "numAccidentesDrogas"
            count { it.positivoAlcohol && it.positivoDrogas } into "numAccidentesAlcoholDrogas"
        }.sortByDesc("numAccidentes")

    println("Accidentes por distrito con alcohol y drogas:")
    println(porDistritoAlcoholDrogas)
    porDistritoAlcoholDrogas.writeCSV(File("reports/accidentesPorDistritoAlcoholDrogas.csv"))


    // Estadisticas por tipo de vehiculo
    val porTipoVehiculo = df.groupBy("tipoVehiculo")
        .aggregate {
            count() into "numAccidentes"
        }
    println("Accidentes por tipo de vehiculo:")
    println(porTipoVehiculo)
    porTipoVehiculo.writeCSV(File("reports/accidentesPorTipoVehiculo.csv"))

    // Estadisticas por distrito
    val porDistritoEstadisticas = porDistritoAlcoholDrogas
        .aggregate {
            sum("numAccidentes") into "numAccidentes"
            sum("numAccidentesAlcohol") into "numAccidentesAlcohol"
            sum("numAccidentesDrogas") into "numAccidentesDrogas"
            max("numAccidentes") into "numAccidentesMax"
            min("numAccidentes") into "numAccidentesMin"
            mean("numAccidentes") into "numAccidentesMedia"
            max("numAccidentesAlcohol") into "numAccidentesAlcoholMax"
            min("numAccidentesAlcohol") into "numAccidentesAlcoholMin"
            mean("numAccidentesAlcohol") into "numAccidentesAlcoholMedia"
            max("numAccidentesDrogas") into "numAccidentesDrogasMax"
            min("numAccidentesDrogas") into "numAccidentesDrogasMin"
            mean("numAccidentesDrogas") into "numAccidentesDrogasMedia"
            max("numAccidentesAlcoholDrogas") into "numAccidentesAlcoholDrogasMax"
            min("numAccidentesAlcoholDrogas") into "numAccidentesAlcoholDrogasMin"
            mean("numAccidentesAlcoholDrogas") into "numAccidentesAlcoholDrogasMedia"
        }
    println("Accidentes por distrito: ")
    println(porDistritoEstadisticas)
    porDistritoEstadisticas.writeJson(File("reports/accidentesPorDistritoEstadisticas.json"))

}