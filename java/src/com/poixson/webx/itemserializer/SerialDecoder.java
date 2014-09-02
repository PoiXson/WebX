package com.poixson.webx.itemserializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;

import com.poixson.commonjava.Utils.utils;
import com.poixson.commonjava.Utils.utilsMath;
import com.poixson.commonjava.Utils.utilsString;
import com.poixson.commonjava.Utils.xString;


public class SerialDecoder {
	protected static final String QUOTE   = "///";
	protected static final String NEWLINE = "/n/";

	protected volatile ItemStack stack = null;




	boolean unsafeEnchants = true;







	public static ItemStack get(final String data) {
		return (new SerialDecoder(data)).getStack();
	}
	public SerialDecoder(final String data) {
		this.decode(data);
	}



	public ItemStack getStack() {
		return this.stack;
	}



	protected void decode(final String data) {
		if(utils.isEmpty(data)) {
			this.stack = null;
			return;
		}
		final xString parse =
			xString.get(data + ",")
				.remove("\r", "\n");
		// parse into key=value, pairs
		final Map<String, String> metaMap = new HashMap<String, String>();
		while(parse.notEmpty()) {
			// get key
			final String key = parse.delim("=").getNext();
			if(utils.isEmpty(key))
				throw new IllegalStateException("Invalid data format, missing key/value pair: "+parse.toString());
			// get value
			final String val;
			if(parse.startsWith(QUOTE))
				val = parse.delim(QUOTE+",").getNext()+QUOTE;
			else
			if(parse.startsWith("{"))
				val = parse.delim("},").getNext()+"}";
			else
				val = parse.delim(",").getNext();
			metaMap.put(key, val);
		}
		// build item
		final Integer id   = utilsMath.toInteger(metaMap.get("id"));
		final String name  = metaMap.get("name");
		final Short damage = utilsMath.toShort(metaMap.get("damage"));
		// this would cause item miss-matches
		//final int amount   = utilsMath.toInt(metaMap.get("amount"), 1);
		metaMap.remove("id");
		metaMap.remove("name");
		metaMap.remove("damage");
		metaMap.remove("amount");
		// find material
		final Material mat = getMaterial(id, name);
		if(mat == null) {
			this.stack = null;
			return;
		}
		// create stack
		final ItemStack stack = new ItemStack(mat);
		if(damage != null && damage.shortValue() >= 0)
			stack.setDurability(damage.shortValue());
		final ItemMeta meta = stack.getItemMeta();
		// written book
		if(metaMap.containsKey("pages")) {
			final BookMeta bookMeta = (BookMeta) meta;
			// book title
			if(metaMap.containsKey("title")) {
				final String title = stripQuotes(metaMap.get("title"));
				if(utils.notEmpty(title))
					bookMeta.setTitle(title);
				metaMap.remove("title");
			}
			// author
			if(metaMap.containsKey("author")) {
				final String author = metaMap.get("author");
				if(utils.notEmpty(author))
					bookMeta.setAuthor(author);
				metaMap.remove("author");
			}
			// pages
			if(metaMap.containsKey("pages")) {
				final int pages = utilsMath.toInteger(metaMap.get("pages"), 0);
				metaMap.remove("pages");
				for(int i = 0; i < pages + 1; i++) {
					final String pageKey =
						"page"+
						utilsString.padFront(2, Integer.toString(i), '0');
					final String page =
						stripQuotes(
							metaMap.get(pageKey)
						);
					if(utils.notEmpty(page)) {
						bookMeta.addPage(
							page.replace(NEWLINE, "\n")
						);
					}
					metaMap.remove(pageKey);
				}
			}
		}
		// apply meta data
		for(final Entry<String, String> entry : metaMap.entrySet()) {
			final String key = entry.getKey();
			final String val = entry.getValue();
			// potion
			if(key.startsWith("potion-effect-")) {
				//TODO: is this data needed?
				continue;
			}
			switch(key) {
			// custom name
			case "display-name": {
				meta.setDisplayName(
					stripQuotes(val)
				);
			}
			break;
			// lore
			case "lore": {
				final String dat = stripQuotes(val);
				final List<String> lore = Arrays.asList(dat.split(NEWLINE));
				meta.setLore(lore);
			}
			break;
			// enchantments
			case "enchants":
			case "stored-enchants": {
				if(val.startsWith("{") && val.endsWith("}")) {
					final xString par = xString.get(
							val.substring(1, val.length() - 1)
					);
					while(par.notEmpty()) {
						final String str = par.delim(",").getNext();
						final String[] parts = str.toUpperCase().split("=", 2);
						if(parts.length != 2) {
System.out.println("Invalid enchant/level pair");
							throw new IllegalStateException();
						}
						final Enchantment ench = Enchantment.getByName(parts[0]);
						final Integer level = utilsMath.toInteger(parts[1]);
						if(ench == null) {
System.out.println("Unknown enchantment: "+str);
							continue;
						}
						if(level == null || level.intValue() < 1) {
System.out.println("Invalid enchantment level: "+str);
							continue;
						}
						// apply enchantment to item
						if(key.equals("enchants")) {
							meta.addEnchant(ench, level.intValue(), this.unsafeEnchants);
						} else
						if(key.equals("stored-enchants")) {
							((EnchantmentStorageMeta) meta).addStoredEnchant(ench, level.intValue(), this.unsafeEnchants);
						} else {
							break;
						}
					}
				}
			}
			break;
			// color
			case "color": {



//TODO: what is this used for?
			}
			break;
			// repair cost
			case "repair-cost": {
				Integer cost = utilsMath.toInteger(val);
				if(cost != null)
					((Repairable) meta).setRepairCost(cost.intValue());
			}
			break;
			// player head
			case "skull-owner": {
				if(utils.notEmpty(val))
					((SkullMeta) meta).setOwner(val);
			}
			break;
			// potion by plugin
			case "custom-effects": {





			}
			break;
			// fireworks
			case "firework-effects": {





			}
			break;
			// ignore these
			case "meta-type":
				break;
			default:
System.out.println("Unknown meta "+key+"="+val);
			}
		}
		// finished building item
		stack.setItemMeta(meta);
		this.stack = stack;
	}



	protected static Material getMaterial(final Integer id, final String name) {
		// item by name
		if(utils.notEmpty(name)) {
			{
				final Material mat = Material.getMaterial(name.toUpperCase());
				if(mat != null) return mat;
			}
			{
				final Material mat = Material.matchMaterial(name);
				if(mat != null) return mat;
			}
		}
		// item by id
		if(id != null && id.intValue() > 0) {
			@SuppressWarnings("deprecation")
			final Material mat = Material.getMaterial(id.intValue());
			if(mat != null) return mat;
		}
		// unknown item type
		return null;
	}



	protected static String stripQuotes(final String str) {
		if(utils.isEmpty(str)) return str;
		if(str.length() < 6)   return str;
		if(!str.startsWith(QUOTE)) return str;
		if(!str.endsWith(QUOTE))   return str;
		return str.substring(3, str.length() - 3);
	}



}
