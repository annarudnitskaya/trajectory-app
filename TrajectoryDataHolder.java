import java.util.List;

public class TrajectoryDataHolder {
        private List<TrajectoryData> data;
        private String fileContent;

        public TrajectoryDataHolder(List<TrajectoryData> data, String fileContent) {
            this.data = data;
            this.fileContent = fileContent;
        }

        public List<TrajectoryData> getData() {
            return data;
        }

        public String getFileContent() {
            return fileContent;
        }
}
