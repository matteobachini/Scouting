//import java.util.*;

/**
 * In this class there are the attribute in common and even those present only in a dataset
 */
public class ReportRaw {
	private int codReport;
    private String review;
    private int userID;
    private int rate;
    private int codPlayer;

    public ReportRaw() {}

    /**
     *
     * @param o
     * @return true if the reports are equal (same title if different object). NON DOVREBBE SERVIRE
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReportRaw reportRaw = (ReportRaw) o;

        //return title != null ? title.equals(reportRaw.title) : reportRaw.title == null;
        return codReport == reportRaw.codReport;
    }

    /*
    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }
    */

    public int getUserID() {
        return userID;
    }

    public String getReview() {
        return review;
    }

    public int getCodReport() {
        return codReport;
    }

    public int getRate() {
        return rate;
    }

    public int getCodPlayer() {
        return codPlayer;
    }

}
