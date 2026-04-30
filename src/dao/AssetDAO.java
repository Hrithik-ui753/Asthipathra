package dao;

import db.DBConnection;
import model.Asset;
import util.BCryptUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AssetDAO {
    public boolean addAsset(Asset asset) {
        if (asset.getCreatedAt() == null || asset.getCreatedAt().isEmpty()) {
            asset.setCreatedAt(util.DateTimeUtil.now());
        }
        String sql = "INSERT INTO Assets(asset_name, asset_type, owner_id, is_encrypted, asset_pin_hash, is_locked, file_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, asset.getAssetName());
            ps.setString(2, asset.getAssetType());
            ps.setInt(3, asset.getOwnerId());
            ps.setInt(4, asset.isEncrypted() ? 1 : 0);
            ps.setString(5, asset.getAssetPinHash());
            ps.setInt(6, asset.isLocked() ? 1 : 0);
            ps.setString(7, asset.getFileUrl());
            ps.setString(8, asset.getCreatedAt());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean updateAsset(Asset asset) {
        String sql = "UPDATE Assets SET asset_name = ?, asset_type = ?, is_encrypted = ?, file_url = ? WHERE asset_id = ? AND owner_id = ? AND is_locked = 0";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, asset.getAssetName());
            ps.setString(2, asset.getAssetType());
            ps.setInt(3, asset.isEncrypted() ? 1 : 0);
            ps.setString(4, asset.getFileUrl());
            ps.setInt(5, asset.getAssetId());
            ps.setInt(6, asset.getOwnerId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean deleteAsset(int assetId, int ownerId) {
        String sql = "DELETE FROM Assets WHERE asset_id = ? AND owner_id = ? AND is_locked = 0";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setInt(2, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Asset> getAssetsByOwner(int ownerId) {
        List<Asset> assets = new ArrayList<>();
        String sql = "SELECT * FROM Assets WHERE owner_id = ? ORDER BY asset_id DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Asset asset = new Asset();
                asset.setAssetId(rs.getInt("asset_id"));
                asset.setAssetName(rs.getString("asset_name"));
                asset.setAssetType(rs.getString("asset_type"));
                asset.setOwnerId(rs.getInt("owner_id"));
                asset.setEncrypted(rs.getInt("is_encrypted") == 1);
                asset.setAssetPinHash(rs.getString("asset_pin_hash"));
                asset.setLocked(rs.getInt("is_locked") == 1);
                asset.setFileUrl(rs.getString("file_url"));
                asset.setCreatedAt(rs.getString("created_at"));
                assets.add(asset);
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return assets;
    }

    public int countAssetsByOwner(int ownerId) {
        String sql = "SELECT COUNT(*) total FROM Assets WHERE owner_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt("total") : 0;
            rs.close();
            return count;
        } catch (SQLException e) {
            return 0;
        }
    }

    public int getNomineeCountForAsset(int assetId) {
        String sql = "SELECT COUNT(*) total FROM Asset_Sharing WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ResultSet rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt("total") : 0;
            rs.close();
            return count;
        } catch (SQLException e) {
            return 0;
        }
    }

    public List<Asset> searchAssets(int ownerId, String query, String type) {
        List<Asset> assets = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM Assets WHERE owner_id = ?");
        
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasType = type != null && !type.trim().isEmpty() && !type.equals("All Types");
        
        if (hasQuery) sql.append(" AND asset_name LIKE ?");
        if (hasType) sql.append(" AND asset_type = ?");
        
        sql.append(" ORDER BY asset_id DESC");
        
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, ownerId);
            if (hasQuery) ps.setString(paramIndex++, "%" + query.trim() + "%");
            if (hasType) ps.setString(paramIndex++, type);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Asset asset = new Asset();
                asset.setAssetId(rs.getInt("asset_id"));
                asset.setAssetName(rs.getString("asset_name"));
                asset.setAssetType(rs.getString("asset_type"));
                asset.setOwnerId(rs.getInt("owner_id"));
                asset.setEncrypted(rs.getInt("is_encrypted") == 1);
                asset.setAssetPinHash(rs.getString("asset_pin_hash"));
                asset.setLocked(rs.getInt("is_locked") == 1);
                asset.setFileUrl(rs.getString("file_url"));
                asset.setCreatedAt(rs.getString("created_at"));
                assets.add(asset);
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return assets;
    }

    public boolean setAssetPin(int assetId, int ownerId, String plainPin) {
        String sql = "UPDATE Assets SET asset_pin_hash = ? WHERE asset_id = ? AND owner_id = ? AND is_locked = 0";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BCryptUtil.hashPassword(plainPin));
            ps.setInt(2, assetId);
            ps.setInt(3, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean verifyAssetPin(int assetId, String plainPin) {
        String sql = "SELECT asset_pin_hash FROM Assets WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("asset_pin_hash");
                return hash != null && BCryptUtil.checkPassword(plainPin, hash);
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return false;
    }

    public boolean setAssetLockState(int assetId, int ownerId, boolean locked) {
        String sql = "UPDATE Assets SET is_locked = ? WHERE asset_id = ? AND owner_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, locked ? 1 : 0);
            ps.setInt(2, assetId);
            ps.setInt(3, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}
