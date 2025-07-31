import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class TrajectoryAppTest {
    private TrajectoryApp app;

    @BeforeEach
    public void setUp() {
        app = new TrajectoryApp();
        app.trajectoryDataList = Arrays.asList(
                new TrajectoryData(new String[]{"1.0", "1.0", "2.0", "3.0", "4.0", "5.0", "6.0"}),
                new TrajectoryData(new String[]{"2.0", "7.0", "8.0", "9.0", "10.0", "11.0", "12.0"}),
                new TrajectoryData(new String[]{"3.0", "13.0", "14.0", "15.0", "16.0", "17.0", "18.0"})
        );
        app.coordCheckboxes = new JCheckBox[3];
        app.speedCheckboxes = new JCheckBox[3];
        for (int i = 0; i < 3; i++) {
            app.coordCheckboxes[i] = new JCheckBox();
            app.speedCheckboxes[i] = new JCheckBox();
        }
    }

    @Test
    public void testCalculateMinMaxYValuesWithCheckboxesSelected() {
        app.coordCheckboxes[0].setSelected(true); // X
        app.speedCheckboxes[0].setSelected(true); // Vx

        app.calculateMinMaxYValues();

        assertEquals(1.0 - (16.0 - 1.0) * 0.1, app.minYValue, 0.001);
        assertEquals(16.0 + (16.0 - 1.0) * 0.1, app.maxYValue, 0.001);
    }

    @Test
    public void testCalculateMinMaxYValuesNoCheckboxesSelected() {
        app.calculateMinMaxYValues();

        assertEquals(0.0, app.minYValue, 0.001);
        assertEquals(1.0, app.maxYValue, 0.001);
    }

    @Test
    public void testGetValueByColumn() {
        TrajectoryData data = new TrajectoryData(new String[]{"1.0", "1.0", "2.0", "3.0", "4.0", "5.0", "6.0"});

        assertEquals(1.0, app.getValueByColumn(data, "X"), 0.001);
        assertEquals(2.0, app.getValueByColumn(data, "Y"), 0.001);
        assertEquals(3.0, app.getValueByColumn(data, "Z"), 0.001);
        assertEquals(4.0, app.getValueByColumn(data, "Vx"), 0.001);
        assertEquals(5.0, app.getValueByColumn(data, "Vy"), 0.001);
        assertEquals(6.0, app.getValueByColumn(data, "Vz"), 0.001);
        assertEquals(1.0, app.getValueByColumn(data, "Unknown"), 0.001); // Default case
    }

    @Test
    public void testGetMaxXValue() {
        double maxXValue = app.getMaxXValue();
        assertEquals(3.0, maxXValue, 0.001);
    }

    @Test
    public void testGetMaxYValue() {
        double maxYValue = app.getMaxYValue();
        assertEquals(18.0, maxYValue, 0.001);
    }

    @Test
    public void testTrajectoryDataHolder() {
        List<TrajectoryData> data = Arrays.asList(
                new TrajectoryData(new String[]{"1.0", "1.0", "2.0", "3.0", "4.0", "5.0", "6.0"}),
                new TrajectoryData(new String[]{"2.0", "7.0", "8.0", "9.0", "10.0", "11.0", "12.0"}),
                new TrajectoryData(new String[]{"3.0", "13.0", "14.0", "15.0", "16.0", "17.0", "18.0"})
        );
        String fileContent = "File content";
        TrajectoryDataHolder dataHolder = new TrajectoryDataHolder(data, fileContent);
        assertEquals(data, dataHolder.getData());
        assertEquals(fileContent, dataHolder.getFileContent());
    }
}

