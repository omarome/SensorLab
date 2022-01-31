package fi.omaralm.sensorlab

import android.content.ContentValues
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Half.EPSILON
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fi.omaralm.sensorlab.ui.theme.SensorLabTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {
    // Create a constant to convert nanoseconds to seconds.
    private val nanoSecTwoSec = 1.0f / 1000000000.0f
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp: Float = 0f

    companion object{
        private lateinit var sensorManager: SensorManager
        private var gyroScopeSensor: Sensor? = null
        val myViewModel = MyViewModel()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroScopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            SensorDataView(myViewModel)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (timestamp != 0f) {
            val dT = (event.timestamp - timestamp) * nanoSecTwoSec
            // Axis of the rotation sample, not normalized yet.

            var axisX: Float = event.values[0]
            var axisY: Float = event.values[1]
            var axisZ: Float = event.values[2]

            myViewModel.updateValue(
                getString(
                    R.string.my_sensor_values,
                    axisX,
                    axisY,
                    axisZ
                )
            )

            // Calculate the angular speed of the sample
            val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

            // Normalize the rotation vector if it's big enough to get the axis`enter code here`
            // (that is, EPSILON should represent your maximum allowable margin of error)
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude
                axisY /= omegaMagnitude
                axisZ /= omegaMagnitude
            }
            val thetaOverTwo: Float = omegaMagnitude * dT / 2.0f
            val sinThetaOverTwo: Float = sin(thetaOverTwo)
            val cosThetaOverTwo: Float = cos(thetaOverTwo)

            deltaRotationVector[0] = sinThetaOverTwo * axisX
            deltaRotationVector[1] = sinThetaOverTwo * axisY
            deltaRotationVector[2] = sinThetaOverTwo * axisZ
            deltaRotationVector[3] = cosThetaOverTwo

        }
        timestamp = event.timestamp.toFloat()
        val deltaRotationMatrix = FloatArray(9) { 0f }
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        // rotationCurrent = rotationCurrent * deltaRotationMatrix;
    }

    override fun onAccuracyChanged(param0: Sensor?, p1: Int) {
        Log.d(ContentValues.TAG, "onAccuracyChanged ${param0?.name}: $p1")
    }

    override fun onResume() {
        super.onResume()
        gyroScopeSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}

@Composable
fun SensorDataView(myViewModel: MyViewModel) {
    val value by myViewModel.value.observeAsState()
    Column( Modifier
        .padding(14.dp),
        horizontalAlignment =  Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.title),
            Modifier
                .background(color = Color.Yellow)
                .align(Alignment.CenterHorizontally)
                .padding(14.dp)
            ,fontSize = 30.sp
        )
        Text(value ?: "",
            Modifier
                .padding( vertical = 10.dp)
                .background(color = Color.LightGray)
                .padding(14.dp)
                .align(Alignment.CenterHorizontally)
            ,fontSize = 30.sp
        )
    }

}

class MyViewModel : ViewModel() {
    private val _value: MutableLiveData<String> = MutableLiveData()
    val value: LiveData<String> = _value

    fun updateValue(value: String) {
        _value.value = value
    }
}

