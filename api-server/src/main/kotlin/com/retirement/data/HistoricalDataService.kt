package com.retirement.data

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HistoricalInflation(val year: Int, val rate: Double)
data class HistoricalReturn(val year: Int, val percent: Double)

object HistoricalDataService {
    private val inflationData = mutableListOf<HistoricalInflation>()
    private val returnsData = mutableListOf<HistoricalReturn>()
    
    // Assuming run from project root or api-server root. We'll try to locate the files.
    // In production/deployment, these might be resources.
    private const val INFLATION_FILE_PATH = "../historical-data/annual-inflation-since-1914.csv"
    private const val RETURNS_FILE_PATH = "../historical-data/s-p-500-annual-returns-since-1928.csv"

    fun loadData() {
        val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

        try {
            val inflationFile = File(INFLATION_FILE_PATH)
            if (inflationFile.exists()) {
                inflationFile.readLines().drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val dateStr = parts[0].trim('"')
                        val date = LocalDate.parse(dateStr, formatter)
                        val rate = parts[1].toDoubleOrNull() ?: 0.0
                        inflationData.add(HistoricalInflation(date.year, rate / 100.0)) // Convert percentage to decimal if needed, but spec says "Inflation Rate" e.g. 1.98. Usually used as 0.0198. Let's assume input is percentage 1.98 -> 0.0198
                    }
                }
            } else {
                println("Warning: Inflation data file not found at ${inflationFile.absolutePath}")
            }

            val returnsFile = File(RETURNS_FILE_PATH)
            if (returnsFile.exists()) {
                returnsFile.readLines().drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val dateStr = parts[0].trim('"')
                        val date = LocalDate.parse(dateStr, formatter)
                        val percent = parts[1].toDoubleOrNull() ?: 0.0
                        returnsData.add(HistoricalReturn(date.year, percent / 100.0)) // Convert percentage to decimal
                    }
                }
            } else {
                println("Warning: Returns data file not found at ${returnsFile.absolutePath}")
            }
            
            println("Loaded ${inflationData.size} inflation records and ${returnsData.size} return records.")

        } catch (e: Exception) {
            println("Error loading historical data: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getInflationData(): List<HistoricalInflation> = inflationData.toList()
    fun getReturnsData(): List<HistoricalReturn> = returnsData.toList()
}
