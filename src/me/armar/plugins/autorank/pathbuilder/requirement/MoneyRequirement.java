package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.language.Lang;
import me.staartvin.utils.pluginlibrary.autorank.Library;
import me.staartvin.utils.pluginlibrary.autorank.hooks.VaultHook;
import org.bukkit.entity.Player;

public class MoneyRequirement extends AbstractRequirement {

    double minMoney = -1;

    @Override
    public String getDescription() {

        String currencyName = "";

        if (this.getAutorank().getDependencyManager().isAvailable(Library.VAULT)) {
            currencyName = VaultHook.getEconomy().currencyNamePlural().trim();
        }

        String lang = Lang.MONEY_REQUIREMENT.getConfigValue(minMoney + " " + currencyName);

        // Check if this requirement is world-specific
        if (this.isWorldSpecific()) {
            lang = lang.concat(" (in world '" + this.getWorld() + "')");
        }

        return lang;
    }

    @Override
    public String getProgressString(final Player player) {

        double money = 0;
        String currencyName = "";

        if (this.getAutorank().getDependencyManager().isAvailable(Library.VAULT)) {
            money = VaultHook.getEconomy().getBalance(player.getPlayer());
            currencyName = VaultHook.getEconomy().currencyNamePlural().trim();
        }

        return money + "/" + minMoney + " " + currencyName;
    }

    @Override
    public boolean meetsRequirement(final Player player) {

        // Check if this requirement is world-specific
        if (this.isWorldSpecific()) {
            // Is player in the same world as specified
            if (!this.getWorld().equals(player.getWorld().getName()))
                return false;
        }

        // If Vault is not available or economy is not set up.
        if (!this.getAutorank().getDependencyManager().isAvailable(Library.VAULT) || VaultHook.getEconomy() == null)
            return false;

        return VaultHook.getEconomy().has(player.getPlayer(), minMoney);
    }

    @Override
    public boolean initRequirement(final String[] options) {

        // Add dependency
        addDependency(Library.VAULT);

        try {
            minMoney = Double.parseDouble(options[0]);
        } catch (final Exception e) {
            this.registerWarningMessage("An invalid number is provided");
            return false;
        }

        if (minMoney < 0) {
            this.registerWarningMessage("No number is provided or smaller than 0.");
            return false;
        }

        return true;
    }

    @Override
    public boolean needsOnlinePlayer() {
        return true;
    }
}
