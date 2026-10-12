public class Cell {
    private boolean isAsexual;
    private int timeFullCounter;
    private int timeStarveCounter;
    private int timeFull;
    private int timeStarve;
    private CellState state;

    public Cell(boolean isAsexual, int timeFull, int timeStarve) {
        this.isAsexual = isAsexual;
        this.timeFull = timeFull;
        this.timeStarve = timeStarve;
        this.state = CellState.FULL;
    }

    public boolean isAsexual() {
        return isAsexual;
    }

    public int getTimeFull() {
        return timeFull;
    }

    public int getTimeStarve() {
        return timeStarve;
    }

    public CellState getState() {
        return state;
    }

    public void tickDown() {
        switch (state) {
            case FULL:
                tickDownFull();
                break;
            case HUNGRY:
                tickDownStarving();
                break;
        }
    }

    private void tickDownStarving() {
        timeStarveCounter--;
        if(timeStarveCounter == 0) {
            state = CellState.DEAD;
        }
    }

    private void tickDownFull() {
        timeFullCounter--;
        if(timeFullCounter <= 0) {
            state = CellState.HUNGRY;
            timeStarveCounter = timeStarve;
        }
    }
}
