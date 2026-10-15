package com.retirement.data

import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HistoricalInflation(val year: Int, val rate: Double)
data class HistoricalReturn(val year: Int, val percent: Double)

/** One historical year with both an S&P 500 return and an inflation rate, as decimals (0.03 = 3%). */
data class HistoricalYear(val year: Int, val equityReturn: Double, val inflation: Double)

object HistoricalDataService {
    private val log = LoggerFactory.getLogger(HistoricalDataService::class.java)

    // Relative to the api-server directory (where ./gradlew run and ./gradlew test run)
    private const val INFLATION_FILE_PATH = "../historical-data/annual-inflation-since-1914.csv"
    private const val SP500_MONTHLY_FILE_PATH = "../historical-data/kaggle-SPX-since1927-monthly.csv"

    private val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

    private class Data(val inflation: List<HistoricalInflation>, val returns: List<HistoricalReturn>)

    // Loaded on first use, so tests and the Monte Carlo engine get the data without calling loadData()
    private val data: Data by lazy { load() }

    /** Load the CSVs now and log what was found (called at server start). */
    fun loadData() {
        log.info("Loaded {} inflation records and {} annual S&P 500 returns", data.inflation.size, data.returns.size)
    }

    fun getInflationData(): List<HistoricalInflation> = data.inflation
    fun getReturnsData(): List<HistoricalReturn> = data.returns

    /** Years that have both a return and an inflation rate, oldest first. */
    fun getHistoricalYears(): List<HistoricalYear> {
        val inflationByYear = data.inflation.associate { it.year to it.rate }
        return data.returns.mapNotNull { r -> inflationByYear[r.year]?.let { HistoricalYear(r.year, r.percent, it) } }
    }

    private fun load(): Data = try {
        Data(loadInflation(File(INFLATION_FILE_PATH)), loadAnnualReturns(File(SP500_MONTHLY_FILE_PATH)))
    } catch (e: Exception) {
        log.error("Error loading historical data", e)
        Data(emptyList(), emptyList())
    }

    /** Annual CSV: year-end date, inflation rate as a percentage (1.98 = 1.98%). */
    private fun loadInflation(file: File): List<HistoricalInflation> {
        if (!file.exists()) {
            log.warn("Inflation data file not found at {}", file.absolutePath)
            return emptyList()
        }
        return csvRows(file).mapNotNull { (date, value) -> value.toDoubleOrNull()?.let { HistoricalInflation(date.year, it / 100.0) } }
    }

    /**
     * Monthly CSV of S&P 500 closes. A year's return is its December close over the prior December close;
     * years without both closes (e.g. a partial final year) are skipped.
     */
    private fun loadAnnualReturns(file: File): List<HistoricalReturn> {
        if (!file.exists()) {
            log.warn("S&P 500 data file not found at {}", file.absolutePath)
            return emptyList()
        }
        val decemberClose = csvRows(file)
            .filter { (date, _) -> date.monthValue == 12 }
            .mapNotNull { (date, value) -> value.toDoubleOrNull()?.let { date.year to it } }
            .toMap()
        return decemberClose.keys.sorted().mapNotNull { year ->
            val prior = decemberClose[year - 1] ?: return@mapNotNull null
            HistoricalReturn(year, decemberClose.getValue(year) / prior - 1.0)
        }
    }

    private fun csvRows(file: File): List<Pair<LocalDate, String>> =
        file.readLines().drop(1).mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size < 2) return@mapNotNull null
            val date = runCatching { LocalDate.parse(parts[0].trim().trim('"'), formatter) }.getOrNull() ?: return@mapNotNull null
            date to parts[1].trim()
        }
}
