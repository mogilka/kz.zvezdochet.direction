package kz.zvezdochet.direction.exporter;

import html.Tag;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.service.AspectTypeService;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Генератор HTML-файлов для экспорта данных
 * @author Nataly Didenko
 *
 */
@SuppressWarnings("unchecked")
public class HTMLExporter {

	/**
	 * Генерация событий периода
	 * @param event событие
	 */
	public void generate(Event event, List<SkyPointAspect> spas) {
		try {
			Tag html = new Tag("html");
			Tag head = new Tag("head");
			head.add(new Tag("meta", "http-equiv=Content-Type content=text/html; charset=UTF-8"));
			head.add(new Tag("link", "href=horoscope_files/horoscope.css rel=stylesheet type=text/css"));
			Tag title = new Tag("title");
			title.add("Прогноз событий");
			head.add(title);
			html.add(head);

			Tag body = new Tag("body");
			body.add(printCopyright());
			Tag table = new Tag("table");
			body.add(table);
			body.add(printCopyright());
			html.add(body);
	
			//события
			Map<Integer, Map<String, List<SkyPointAspect>>> map = new HashMap<Integer, Map<String, List<SkyPointAspect>>>();
			for (SkyPointAspect spa : spas) {
				int age = (int)spa.getAge();
				Map<String, List<SkyPointAspect>> agemap = map.get(age);
				if (null == agemap) {
					agemap = new HashMap<String, List<SkyPointAspect>>();
					agemap.put("main", new ArrayList<SkyPointAspect>());
					agemap.put("strong", new ArrayList<SkyPointAspect>());
					agemap.put("inner", new ArrayList<SkyPointAspect>());
				}
				String code = spa.getAspect().getType().getCode();
				if (code.equals("NEUTRAL") || code.equals("NEGATIVE") || code.equals("POSITIVE")) {
					if (spa.getSkyPoint2() instanceof Planet) {
							List<SkyPointAspect> list = agemap.get("inner");
							list.add(spa);
					} else {
						if (code.equals("NEUTRAL")) {
							List<SkyPointAspect> list = agemap.get("main");
							list.add(spa);
						} else {
							List<SkyPointAspect> list = agemap.get("strong");
							list.add(spa);
						}
					}
				}
				map.put(age, agemap);
			}
			for (Map.Entry<Integer, Map<String, List<SkyPointAspect>>> entry : map.entrySet()) {
			    int age = entry.getKey();
			    Map<String, List<SkyPointAspect>> agemap = entry.getValue();
				for (Map.Entry<String, List<SkyPointAspect>> subentry : agemap.entrySet())
					generateEvents(event, table, age, subentry.getKey(), subentry.getValue());
			}
			
			if (html != null) {
//				System.out.println(html);
				export(html.toString());
			}
		} catch(Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Ошибка", e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Экспорт данных в файл
	 * @param html-файл
	 * @todo использовать конфиг для задания пути
	 */
	private void export(String html) {
		try {
			String datafile = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/events.html").getPath(); //$NON-NLS-1$
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter( 
				new FileOutputStream(datafile), "UTF-8"));
			writer.append(html);
			writer.close();
			//TODO показывать диалог, что документ сформирован
			//а ещё лучше открывать его
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Экспорт завершён");
		}
	}

	/**
	 * Отображение информации о копирайте
	 * @return html-тег с содержимым
	 */
	private Tag printCopyright() {
		Tag cell = new Tag("div", "class=copyright");
		cell.add("&copy; 1998-");
		Tag script = new Tag("script", "type=text/javascript");
		script.add("var year = new Date().getYear(); if (year < 1000) year += 1900; document.write(year);");
		cell.add(script);
		cell.add("Астрологический сервис" + "&nbsp;");
		Tag a = new Tag("a", "href=http://zvezdochet.guru/ target=_blank");
		a.add("«Звездочёт»");
		cell.add(a);
		cell.add(" &mdash; Взгляни на себя в прошлом, настоящем и будущем");
		return cell;
	}

	/**
	 * Генерация событий по категориям
	 */
	private void generateEvents(Event event, Tag table, int age, String code, List<SkyPointAspect> spas) {
		try {
			Tag row = new Tag("tr");
			Tag cell = new Tag("td", "class=header");
			String header = "";
			if (code.equals("main"))
				header = "Главные события";
			else if (code.equals("strong"))
				header = "Менее значимые события";
			else if (code.equals("inner"))
				header = "Проявления личности";
			cell.add(CoreUtil.getAgeString(age) + ": " + header);
			row.add(cell);
			table.add(row);

			row = new Tag("tr");
			cell = new Tag("td");
			DirectionService service = new DirectionService();
			DirectionAspectService servicea = new DirectionAspectService();
			boolean child = age < event.MAX_TEEN_AGE;
			for (SkyPointAspect spa : spas) {
				AspectType type = spa.getAspect().getType();
				String tcode = type.getCode();
				if (tcode.contains("HIDDEN")) {
					if (tcode.contains("NEGATIVE"))
						type = (AspectType)new AspectTypeService().find("NEGATIVE");
					else if (tcode.contains("POSITIVE"))
						type = (AspectType)new AspectTypeService().find("POSITIVE");
				}
				Tag h5 = new Tag("h5");

				Planet planet = (Planet)spa.getSkyPoint1();
				SkyPoint skyPoint = spa.getSkyPoint2();
				if (skyPoint instanceof House) {
					House house = (House)skyPoint;
					h5.add(planet.getShortName() + " " + type.getSymbol() + " " + house.getShortName());
					cell.add(h5);

					DirectionText dirText = (DirectionText)service.find(planet, house, type);
					if (dirText != null) {
						Tag li = new Tag("div", "style=color:" + type.getFontColor());
						li.add(dirText.getText());
						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders)
							li.add(gender.getText());
						cell.add(li);
					}
				} else if (skyPoint instanceof Planet) {
					Planet planet2 = (Planet)skyPoint;
					h5.add(planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName());
					cell.add(h5);

					PlanetAspectText dirText = (PlanetAspectText)servicea.find(planet, planet2, type);
					if (dirText != null) {
						Tag li = new Tag("div", "style=color:" + type.getFontColor());
						li.add(dirText.getText());
						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders)
							li.add(gender.getText());
						cell.add(li);
					}
				}
			}
			row.add(cell);
			table.add(row);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
