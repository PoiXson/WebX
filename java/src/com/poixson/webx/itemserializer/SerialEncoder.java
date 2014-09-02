package com.poixson.webx.itemserializer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;

import com.poixson.commonjava.Utils.utils;
import com.poixson.commonjava.Utils.utilsObject;
import com.poixson.commonjava.Utils.utilsString;


public class SerialEncoder {
	protected static final String QUOTE   = "///";
	protected static final String NEWLINE = "/n/";

	protected volatile String data = null;



	public static String get(final ItemStack stack) {
		return (new SerialEncoder(stack)).getData();
	}
	public SerialEncoder(final ItemStack stack) {
		this.encode(stack);
	}



	public String getData() {
		return this.data;
	}



	protected void encode(final ItemStack stack) {
		if(stack == null) {
			this.data = null;
			return;
		}
		// sort meta by keys
		final Map<String, String> sortedSerial = new TreeMap<String, String>();
		final Material mat = stack.getType();
		if(Material.AIR.equals(mat)) {
			this.data = null;
			return;
		}
		// general data
		sortedSerial.put("id",     Integer.toString(getItemId(stack)));
		sortedSerial.put("name",   mat.name());
		sortedSerial.put("damage", Short.toString(stack.getDurability()));
		// this would cause item miss-matches
		//sortedSerial.put("amount", Integer.toString(stack.getAmount()));
		// get meta data
		final ItemMeta itemMeta = stack.getItemMeta();
		if(itemMeta != null) {
			final Map<String, Object> metaMap = itemMeta.serialize();
			// written book
			boolean isbook = false;
			if(itemMeta instanceof BookMeta) {
				isbook = true;
				final BookMeta bookMeta = (BookMeta) itemMeta;
				final String title  = bookMeta.getTitle();
				final String author = bookMeta.getAuthor();
				final int pageCount = bookMeta.getPageCount();
				// book title
				if(utils.notEmpty(title)) {
					sortedSerial.put(
						"pages",
						(new StringBuilder())
							.append(QUOTE)
							.append(title.replace(QUOTE, ""))
							.append(QUOTE)
							.toString()
					);
				}
				// author
				if(utils.notEmpty(author)) {
					sortedSerial.put(
						"author",
						author.replace(",", " ")
					);
				}
				// pages
				if(pageCount > 0) {
					sortedSerial.put("pages", Integer.toString(pageCount));
					for(int index = 1 ; index < pageCount+1 ; index++) {
						final String page = bookMeta.getPage(index);
						sortedSerial.put(
							"page"+utilsString.padFront(2, Integer.toString(index), '0'),
							(new StringBuilder())
								.append(QUOTE)
								.append(
									page.replace(QUOTE, "")
										.replace(NEWLINE, "")
										.replace("\n", NEWLINE))
								.append(QUOTE)
								.toString()
						);
					}
				}
			}
			// encode meta data
			final Iterator<Entry<String, Object>> it = metaMap.entrySet().iterator();
			while(it.hasNext()) {
				final Entry<String, Object> metaEntry = it.next();
				final String key = metaEntry.getKey().toLowerCase();
				final Object val = metaEntry.getValue();
				if(isbook) {
					if("title".equals(key))  continue;
					if("author".equals(key)) continue;
					if("pages".equals(key))  continue;
				}
				switch(key) {
				// meta type
				case "meta-type": {
					final String type = (String) val;
					if(utils.notEmpty(type))
						sortedSerial.put(key, type);
					// potion (not water bottle)
					if(type.equalsIgnoreCase("potion") && stack.getDurability() != 0) {
						final Potion potion = Potion.fromItemStack(stack);
//						final String potionName = potion.getType().name();
						final TreeSet<String> sortedEffects = new TreeSet<String>();
						for(final PotionEffect effect : potion.getEffects()) {
							final String effectName = effect.getType().getName();
							final int effectAmp = effect.getAmplifier();
							final int effectDur = effect.getDuration();
							sortedEffects.add(
								(new StringBuilder())
									.append("{")
									.append("effect=")   .append(effectName).append(",")
									.append("amplifier=").append(effectAmp) .append(",")
									.append("duration=") .append(effectDur)
									.append("}")
									.toString()
							);
						}
						int index = 0;
						for(final String str : sortedEffects) {
							sortedSerial.put(
								(new StringBuilder())
									.append(potion.isSplash() ? "splash-" : "")
									.append("potion-effect-")
									.append(index++)
									.toString(),
								str
							);
						}
					}
				}
				break;
				// custom name
				case "display-name": {
					final String display = (String) val;
					if(utils.notEmpty(display)) {
						sortedSerial.put(
							key,
							(new StringBuilder())
								.append(QUOTE)
								.append(display.replace(QUOTE, ""))
								.append(QUOTE)
								.toString()
						);
					}
				}
				break;
				// lore
				case "lore": {
					final List<String> lore = utilsObject.castList(String.class, val);
					if(utils.notEmpty(lore)) {
						final StringBuilder str = new StringBuilder();
						str.append(QUOTE);
						int index = 0;
						for(final String line : lore) {
							if(index++ != 0)
								str.append("/n/");
							str.append(
								line.replace(QUOTE, "")
									.replace(NEWLINE, "")
									.replace("\n", NEWLINE)
							);
						}
						str.append(QUOTE);
						sortedSerial.put(
							key,
							str.toString()
						);
					}
				}
				break;
				// enchantments
				case "enchants":
				case "stored-enchants": {
					final Map<Enchantment, Integer> enchants;
					if(key.equalsIgnoreCase("enchants")) {
						enchants = stack.getEnchantments();
					} else
					if(key.equalsIgnoreCase("stored-enchants")) {
						enchants = ((EnchantmentStorageMeta) stack.getItemMeta())
							.getStoredEnchants();
					} else {
						break;
					}
					if(utils.notEmpty(enchants)) {
						// sort enchants by name
						final SortedMap<String, Integer> sorted = new TreeMap<String, Integer>();
						for(final Entry<Enchantment, Integer> entry : enchants.entrySet())
							sorted.put(
								entry.getKey().getName(),
								entry.getValue()
							);
						final StringBuilder str = new StringBuilder();
						str.append("{");
						int index = 0;
						for(final Entry<String, Integer> entry : sorted.entrySet()) {
							if(index++ != 0)
								str.append(",");
							str.append(entry.getKey())
								.append("=")
								.append(entry.getValue().toString());
						}
						str.append("}");
						sortedSerial.put(
							key,
							str.toString()
						);
					}
				}
				break;
				// color
				case "color": {
//					final String color = (String) meta.get("color");
//					if(!utils.isEmpty(color)) {
//						str.append("color=")
//							.append(color.replace("Color:rgb0x", ""))
//							.append(",");
//					}
				}
				// repair cost
				case "repair-cost": {
					int cost = 0;
					if(val instanceof Repairable) {
						final Repairable repair = (Repairable) val;
						cost = repair.getRepairCost();
					} else {
						cost = ((Integer) val).intValue();
					}
					if(cost > 0) {
						sortedSerial.put(
							key,
							Integer.toString(cost)
						);
					}
				}
				break;
				// player head
				case "skull-owner": {
					final String owner = (String) val;
					if(utils.notEmpty(owner))
						sortedSerial.put(
							key,
							owner.replace(",", "")
						);
				}
				break;
				// potion by plugin
				case "custom-effects": {
System.out.println("++++++++++++++++++++POTION CUSTOM EFFECT: "+key+" "+val.toString());
				}
				break;
				// fireworks
				case "firework-effects": {
					final FireworkMeta fireworkMeta = (FireworkMeta) val;
//					fireworkMeta.getPower();
					for(final FireworkEffect effect : fireworkMeta.getEffects()) {
System.out.println("FIREWORK-TYPE:  "+effect.getType().name());
System.out.println("FIREWORK-COLOR: "+effect.getColors());
System.out.println("FIREWORK-FADE:  "+effect.getFadeColors());
					}
				}
				break;
				default:
System.out.println("====="+(new StringBuilder("Unknown meta: ")).append(key).append(" = ").append(val));
				break;
				} /* end switch */
			} /* end while */
		} /* end itemMeta */
		// build serialized data
		final StringBuilder strOut = new StringBuilder();
		int index = 0;
		for(final Entry<String, String> entry : sortedSerial.entrySet()) {
			if(index++ != 0)
				strOut.append(",");
			strOut.append(entry.getKey())
				.append("=")
				.append(entry.getValue());
		}
		this.data = strOut.toString();
	}



	@SuppressWarnings("deprecation")
	protected static int getItemId(final ItemStack stack) {
		if(stack == null) throw new NullPointerException();
		return stack.getTypeId();
	}



}
