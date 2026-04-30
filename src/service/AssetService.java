package service;

import dao.ActivityDAO;
import dao.AssetDAO;
import dao.ReleaseDAO;
import model.Asset;
import util.AESUtil;
import util.DateTimeUtil;
import util.BCryptUtil;

import java.util.List;

public class AssetService {
    private final AssetDAO assetDAO = new AssetDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();
    private final ReleaseDAO releaseDAO = new ReleaseDAO();

    public boolean addAsset(String name, String type, int ownerId, boolean encrypted, String fileUrl, String pin) {
        if (pin == null || !pin.matches("\\d{5}")) {
            return false;
        }
        Asset asset = new Asset();
        asset.setAssetName(name);
        asset.setAssetType(type);
        asset.setOwnerId(ownerId);
        asset.setEncrypted(encrypted);
        asset.setAssetPinHash(BCryptUtil.hashPassword(pin));
        asset.setLocked(false);
        asset.setFileUrl(encrypted ? AESUtil.encrypt(fileUrl) : fileUrl);
        asset.setCreatedAt(DateTimeUtil.now());
        boolean ok = assetDAO.addAsset(asset);
        if (ok) {
            // Get the generated ID (assuming it's the last inserted or fetch it)
            // For simplicity, let's assume assetDAO.addAsset could return the asset with ID or we fetch it.
            // Since addAsset returns boolean, we might need to get the latest asset ID for this owner.
            List<Asset> assets = assetDAO.getAssetsByOwner(ownerId);
            if (!assets.isEmpty()) {
                int newAssetId = assets.get(0).getAssetId(); // Latest added
                releaseDAO.addDefaultInactivityCondition(newAssetId);
            }
            activityDAO.logAudit(ownerId, "Asset added: " + name, DateTimeUtil.now());
            activityDAO.addNotification(ownerId, "Asset added successfully: " + name, DateTimeUtil.now());
            activityDAO.addPoints(ownerId, encrypted ? 15 : 10);
        }
        return ok;
    }

    public List<Asset> getAssets(int ownerId) {
        List<Asset> list = assetDAO.getAssetsByOwner(ownerId);
        for (Asset a : list) {
            if (a.isEncrypted()) {
                a.setFileUrl(AESUtil.decrypt(a.getFileUrl()));
            }
        }
        return list;
    }

    public boolean deleteAsset(int assetId, int ownerId) {
        boolean ok = assetDAO.deleteAsset(assetId, ownerId);
        if (ok) {
            activityDAO.logAudit(ownerId, "Asset deleted: " + assetId, DateTimeUtil.now());
        }
        return ok;
    }

    public int countAssets(int ownerId) {
        return assetDAO.countAssetsByOwner(ownerId);
    }

    public void attachDefaultReleaseCondition(int assetId) {
        releaseDAO.addDefaultInactivityCondition(assetId);
    }

    public List<Asset> searchAssets(int ownerId, String query, String type) {
        List<Asset> list = assetDAO.searchAssets(ownerId, query, type);
        for (Asset a : list) {
            if (a.isEncrypted()) {
                a.setFileUrl(AESUtil.decrypt(a.getFileUrl()));
            }
        }
        return list;
    }

    public int getNomineeCountForAsset(int assetId) {
        return assetDAO.getNomineeCountForAsset(assetId);
    }

    public boolean verifyPin(int assetId, String pin) {
        return assetDAO.verifyAssetPin(assetId, pin);
    }

    public boolean lockAsset(int assetId, int ownerId) {
        return assetDAO.setAssetLockState(assetId, ownerId, true);
    }
}
