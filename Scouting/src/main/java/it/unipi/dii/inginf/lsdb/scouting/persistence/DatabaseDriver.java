package it.unipi.dii.inginf.lsdb.scouting.persistence;

/**
 * Interface with the method that every database driver must implement
 */
public interface DatabaseDriver {
    public boolean initConnection();
    public void closeConnection();
}
