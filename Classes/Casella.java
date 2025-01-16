package Classes;

public class Casella
{
    
    private boolean isOccupato; // è occupato
    private Boolean ownershipFlag;  //di chi è

    public Casella()
    {
        this.isOccupato = false;
        this.ownershipFlag = null;
    }

    //GETTERS & SETTERS

    public void setOccupato(boolean isOccupato)
    {
        this.isOccupato = isOccupato;
    }

    public void setOwner(boolean owner)
    {
        this.ownershipFlag = owner;
    }

    public boolean isOccupato()
    {
        return this.isOccupato;
    }

    public Boolean getOwnershipFlag()
    {
        return this.ownershipFlag;
    }
    
}
