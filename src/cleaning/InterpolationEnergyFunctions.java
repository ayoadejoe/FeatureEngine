package cleaning;

import java.time.Duration;
import java.time.LocalDateTime;

public class InterpolationEnergyFunctions {

    double getStepValue(double previousEnergy, double currentEnergy, LocalDateTime previousTime, LocalDateTime currentTime){
        long timeDifference = Duration.between(previousTime, currentTime).toMinutes();
        double energyDifference = currentEnergy - previousEnergy;
        return energyDifference/timeDifference;
    }

    double getPower(double previousEnergy, double interpolatedEnergy){
        double energyDifference = interpolatedEnergy - previousEnergy;
        return energyDifference*60000;      //per minute power
    }

    double getCurrent(double power, double pf, double voltage){
        return power/(voltage*pf);
    }

    double generateInterpolatedEnergy(double stepValue, double previousEnergy){
        return previousEnergy+stepValue;
    }

}
