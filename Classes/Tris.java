package Classes;

public class Tris {
    public final Casella[][] tris;

    public Tris() 
    {
        this.tris = new Casella[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                this.tris[i][j] = new Casella();
    }

    public boolean occupaCasella(int row, int col, boolean owner) 
    {
        if (!this.tris[row][col].isOccupato()) {
            this.tris[row][col].setOccupato(true);
            this.tris[row][col].setOwner(owner);
            return true;
        }
        return false;
    }

    public boolean checkWin(boolean player) 
    {
        // Controlla le righe
        for (int i = 0; i<3; i++) 
        {
            if (tris[i][0].isOccupato() && tris[i][1].isOccupato() && tris[i][2].isOccupato() &&
                tris[i][0].getOwnershipFlag() == player && tris[i][1].getOwnershipFlag() == player && tris[i][2].getOwnershipFlag() == player) {
                return true;
            }
        }

        // Controlla le colonne
        for (int j = 0; j<3; j++) 
        {
            if (tris[0][j].isOccupato() && tris[1][j].isOccupato() && tris[2][j].isOccupato() &&
                tris[0][j].getOwnershipFlag() == player && tris[1][j].getOwnershipFlag() == player && tris[2][j].getOwnershipFlag() == player) {
                return true;
            }
        }

        // Controlla la diagonale principale (da dx a sx)
        if (tris[0][0].isOccupato() && tris[1][1].isOccupato() && tris[2][2].isOccupato() &&
            tris[0][0].getOwnershipFlag() == player && tris[1][1].getOwnershipFlag() == player && tris[2][2].getOwnershipFlag() == player) {
            return true;
        }

        // Controlla la diagonale secondaria (da dx a sx)
        if (tris[0][2].isOccupato() && tris[1][1].isOccupato() && tris[2][0].isOccupato() &&
            tris[0][2].getOwnershipFlag() == player && tris[1][1].getOwnershipFlag() == player && tris[2][0].getOwnershipFlag() == player) {
            return true;
        }

        //Non ho trovato nessuna win condition
        return false;
    }

    public boolean isFull() 
    {
        for (int i = 0; i<3; i++) 
        {
            for (int j = 0; j<3; j++) 
            {
                if (!tris[i][j].isOccupato())
                {
                    return false;
                }
            }
        }
        return true;
    }
}
