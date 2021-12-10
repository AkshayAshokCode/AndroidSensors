package com.akshayAshokCode.androidsensors.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.akshayAshokCode.androidsensors.data.SensorRepository
import kotlinx.coroutines.Dispatchers

class AllSensorsViewModel :ViewModel() {
    private var sensorRepository=SensorRepository()
    var sensors= liveData(Dispatchers.IO){
        val result=sensorRepository.getSensors()
        emit(result)
    }
}