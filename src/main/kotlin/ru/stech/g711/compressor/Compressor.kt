package ru.stech.g711.compressor

abstract class Compressor {
   abstract fun compress(sample: Short): Int
}
