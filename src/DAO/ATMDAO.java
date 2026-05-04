package DAO;

import models.ATM;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ATMDAO {

    public ATM getATMById(String atmId) {
        ATM atm = null;
        String query = "SELECT * FROM ATMs WHERE ATMID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, atmId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                atm = new ATM(
                        rs.getString("ATMID"),
                        rs.getString("Location"),
                        rs.getInt("Count500k"),
                        rs.getInt("Count200k"),
                        rs.getInt("Count100k"),
                        rs.getInt("Count50k"),
                        rs.getDouble("TotalCash"),
                        rs.getString("Status")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return atm;
    }

    public boolean updateATMCash(ATM atm) {
        // TotalCash tự động tính toán trong MySQL nên không cần UPDATE cột đó
        String query = "UPDATE ATMs SET Count500k=?, Count200k=?, Count100k=?, Count50k=? WHERE ATMID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, atm.getCount500k());
            pstmt.setInt(2, atm.getCount200k());
            pstmt.setInt(3, atm.getCount100k());
            pstmt.setInt(4, atm.getCount50k());
            pstmt.setString(5, atm.getAtmId());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}