public class TrajectoryData {
        private String time;
        private double x, y, z;
        private double vx, vy, vz;

        public TrajectoryData(String[] data) throws NumberFormatException {
            this.time = data[0];
            try {
                this.x = Double.parseDouble(data[1]);
                this.y = Double.parseDouble(data[2]);
                this.z = Double.parseDouble(data[3]);
                this.vx = Double.parseDouble(data[4]);
                this.vy = Double.parseDouble(data[5]);
                this.vz = Double.parseDouble(data[6]);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing number: " + e.getMessage());
                this.x = 0.0; // Default value
                this.y = 0.0;
                this.z = 0.0;
                this.vx = 0.0;
                this.vy = 0.0;
                this.vz = 0.0;
            }
        }

        // Геттеры и сеттеры
        public String getTime() { return time; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public double getVx() { return vx; }
        public double getVy() { return vy; }
        public double getVz() { return vz; }

        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
        public void setZ(double z) { this.z = z; }
        public void setVx(double vx) { this.vx = vx; }
        public void setVy(double vy) { this.vy = vy; }
        public void setVz(double vz) { this.vz = vz; }


    public void setTime(String time) {
        this.time = time;
    }
}
