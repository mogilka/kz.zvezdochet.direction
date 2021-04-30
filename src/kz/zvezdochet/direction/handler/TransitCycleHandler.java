package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import org.jfree.data.time.TimeSeriesDataItem;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.ChapterAutoNumber;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.analytics.bean.PlanetText;
import kz.zvezdochet.analytics.service.PlanetTextService;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Ingress;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectTypeService;

/**
 * Генерация планетарных циклов за указанный период
 * @author Natalie Didenko
 */
public class TransitCycleHandler extends Handler {
	private BaseFont baseFont;

	public TransitCycleHandler() {
		super();
		try {
			baseFont = PDFUtil.getBaseFont();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Execute
	public void execute(@Active MPart activePart) {
		Document doc = new Document();
		try {
			long duration = System.currentTimeMillis();
			long run = duration;
			TransitPart periodPart = (TransitPart)activePart.getObject();
			if (!periodPart.check(0)) return;

			Event person = periodPart.getPerson();
			Place place = periodPart.getPlace();

			int choice = DialogUtil.alertQuestion("Вопрос", "Выберите тип прогноза:", new String[] {"Реалистичный", "Оптимистичный"});
			boolean optimistic = choice > 0;

			//Признак использования астрологических терминов
			boolean term = periodPart.isTerm();
			updateStatus("Расчёт циклов на период", false);

			Date initDate = periodPart.getInitialDate();
			Date finalDate = periodPart.getFinalDate();
			Calendar start = Calendar.getInstance();
			start.setTime(initDate);
			Calendar end = Calendar.getInstance();
			end.setTime(finalDate);
			end.add(Calendar.DATE, 1);
			end.set(Calendar.HOUR_OF_DAY, 0);
			end.set(Calendar.MINUTE, 0);
			end.set(Calendar.SECOND, 0);
			long startime = initDate.getTime();
			long endtime = finalDate.getTime();

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/cycle.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler());
	        doc.open();

	    	Font font = PDFUtil.getRegularFont();

	        //metadata
	        PDFUtil.getMetaData(doc, "Важные периоды");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Важные периоды");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Важные периоды", null);
			chapter.add(p);

			String text = person.getCallname() + ", прогноз на период: ";
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
			SimpleDateFormat spf = new SimpleDateFormat("d MMMM");
			text += sdf.format(initDate);
			boolean days = (DateUtil.getDateFromDate(initDate) != DateUtil.getDateFromDate(finalDate)
				|| DateUtil.getMonthFromDate(initDate) != DateUtil.getMonthFromDate(finalDate)
				|| DateUtil.getYearFromDate(initDate) != DateUtil.getYearFromDate(finalDate));
			if (days)
				text += " — " + sdf.format(finalDate);
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			if (null == place)
				place = new Place().getDefault();
			boolean pdefault = place.getId().equals(place.getDefault().getId());
			
			text = "Тип прогноза: " + (optimistic ? "оптимистичный" : "реалистичный");
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			Font fontgray = PDFUtil.getAnnotationFont(false);
			text = "Дата составления: " + DateUtil.fulldtf.format(new Date());
			p = new Paragraph(text, fontgray);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			p = new Paragraph();
	        p.setAlignment(Element.ALIGN_CENTER);
			p.setSpacingAfter(20);
	        p.add(new Chunk("Автор: ", fontgray));
	        Chunk chunk = new Chunk(PDFUtil.AUTHOR, new Font(baseFont, 10, Font.UNDERLINE, PDFUtil.FONTCOLOR));
	        chunk.setAnchor(PDFUtil.WEBSITE);
	        p.add(chunk);
	        chapter.add(p);

			p = new Paragraph();
			p.add(new Chunk("Файл содержит большой объём информации, и нет смысла пытаться его весь прочитать. "
				+ "Используйте прогноз в начале каждого месяца как путеводитель, помогающий понять тенденции и учесть риски.", font));
			chapter.add(p);
			chapter.add(Chunk.NEWLINE);

			Font red = PDFUtil.getDangerFont();
			p = new Paragraph();
			String divergence = person.isRectified() ? "1 день" : "2 дня";
			p.add(new Chunk("Общая погрешность прогноза составляет ±" + divergence + ". ", red));
			chapter.add(p);
			chapter.add(Chunk.NEWLINE);

	        if (!pdefault) {
	        	divergence = person.isRectified() ? "2 дня" : "3 дня";
				chapter.add(new Paragraph("Прогноз сделан для локации «" + place.getName() + "». "
					+ "Если в течение прогнозного периода вы переедете в более отдалённое место (в другой часовой пояс или с ощутимой сменой географической широты), "
					+ "то погрешность некоторых прогнозов может составить ±" + divergence + ".", font));
				chapter.add(Chunk.NEWLINE);
	        }

			chapter.add(new Paragraph("Если длительность прогноза исчисляется днями, неделями и месяцами, то это не значит, что каждый день будет что-то происходить. "
	        	+ "Просто вероятность описанных событий будет сохраняться в течение всего периода. "
	        	+ "Чаще всего прогноз ярко проявляет себя в первый же день периода, но может сбыться и позже.", font));
			chapter.add(Chunk.NEWLINE);
	        doc.add(chapter);

			Map<Integer, Map<Integer, List<Long>>> years = new TreeMap<Integer, Map<Integer, List<Long>>>();
			Map<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>> hyears = new TreeMap<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>>();
			Map<Integer, Map<Integer, Map<Long, List<TimeSeriesDataItem>>>> myears = new TreeMap<Integer, Map<Integer, Map<Long, List<TimeSeriesDataItem>>>>();
			Map<Integer, Map<Integer, Map<Long, Map<String, List<Object>>>>> texts = new TreeMap<Integer, Map<Integer, Map<Long, Map<String, List<Object>>>>>();

			System.out.println("Prepared for: " + (System.currentTimeMillis() - run));
			run = System.currentTimeMillis();

			//разбивка дат по годам и месяцам
			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				int y = calendar.get(Calendar.YEAR);
				int m = calendar.get(Calendar.MONTH);
				
				Map<Integer, List<Long>> months = years.containsKey(y) ? years.get(y) : new TreeMap<Integer, List<Long>>();
				List<Long> dates = months.containsKey(m) ? months.get(m) : new ArrayList<Long>();
				long time = calendar.getTimeInMillis(); 
				if (!dates.contains(time))
					dates.add(time);
				Collections.sort(dates);
				months.put(m, dates);
				years.put(y, months);

				Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = hyears.containsKey(y) ? hyears.get(y) : new TreeMap<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>();
				months2.put(m, new TreeMap<Long, Map<Long, List<TimeSeriesDataItem>>>());
				hyears.put(y, months2);

				Map<Integer, Map<Long, List<TimeSeriesDataItem>>> months3 = myears.containsKey(y) ? myears.get(y) : new TreeMap<Integer, Map<Long, List<TimeSeriesDataItem>>>();
				months3.put(m, new TreeMap<Long, List<TimeSeriesDataItem>>());
				myears.put(y, months3);
			}

			Map<String, List<DatePeriod>> periods = new HashMap<String, List<DatePeriod>>();
			/**
			 * коды ингрессий, используемых в отчёте
			 */
			String[] icodes = new String[] {
				Ingress._EXACT, Ingress._EXACT_HOUSE,
				Ingress._SEPARATION, Ingress._SEPARATION_HOUSE,
				Ingress._REPEAT, Ingress._REPEAT_HOUSE,
				Ingress._RETRO
			};

			//создаём аналогичный массив, но с домами вместо дат
			for (Map.Entry<Integer, Map<Integer, List<Long>>> entry : years.entrySet()) {
				int y = entry.getKey();
				Map<Integer, List<Long>> months = years.get(y);
				Map<Integer, Map<Long, Map<String, List<Object>>>> mtexts = texts.get(y);
				if (null == mtexts)
					mtexts = new TreeMap<Integer, Map<Long, Map<String, List<Object>>>>();

				//считаем транзиты
				for (Map.Entry<Integer, List<Long>> entry2 : months.entrySet()) {
					int m = entry2.getKey();

					List<Long> dates = months.get(m);
					Map<Long, Map<String, List<Object>>> dtexts = mtexts.get(m);
					if (null == dtexts)
						dtexts = new TreeMap<Long, Map<String, List<Object>>>();

					for (Long time : dates) {
						Date date = new Date(time);
//						System.out.println(date + ", " + finalDate);
//						System.out.println(time + ", " + endtime);
						boolean firstdate = (time == startime);
						boolean lastdate = (time == endtime);
						String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 12:00:00";
						Event event = new Event();
						Date edate = DateUtil.getDatabaseDateTime(sdate);
						event.setBirth(edate);
						event.setPlace(place.getDefault());
						event.setZone(0);
						event.calc(true);

						Map<String, List<Object>> ingressList = person.initIngresses(event);
						if (ingressList.isEmpty())
							continue;

						Map<String, List<Object>> ingressmap = new TreeMap<String, List<Object>>();
						for (Map.Entry<String, List<Object>> daytexts : ingressList.entrySet()) {
							String key = daytexts.getKey();
							if (!Arrays.asList(icodes).contains(key))
								continue;

							List<Object> objects = daytexts.getValue();
							if (objects.isEmpty())
								continue;

							List<Object> objects2 = ingressmap.containsKey(key) ? ingressmap.get(key) : new ArrayList<Object>();
							String[] negatives = {"Kethu", "Lilith"};
							String[] giants = {"Jupiter", "Saturn"};
							for (Object object : objects) {
								if (object instanceof SkyPointAspect) {
									SkyPointAspect spa = (SkyPointAspect)object;
									Planet planet = (Planet)spa.getSkyPoint1();
									if (planet.getCode().equals("Moon"))
										continue;

//									if (planet.getCode().equals("Venus"))
//										System.out.println();

									if (planet.isMain() && !spa.isRetro())
										continue;

									SkyPoint skyPoint = spa.getSkyPoint2();
									String acode = spa.getAspect().getCode();
									if (skyPoint instanceof House
											&& !acode.equals("CONJUNCTION"))
										continue;

									if (!acode.equals("CONJUNCTION")) {
										if (planet.isFictitious())
											continue;

										if (skyPoint instanceof Planet
												&& ((Planet)skyPoint).isFictitious())
											continue;

										if (Arrays.asList(giants).contains(planet.getCode())
												&& !spa.isRetro())
											continue;
									}
									if (optimistic) {
										if (2 == spa.getAspect().getTypeid())
											continue;

										if (acode.equals("CONJUNCTION")) {
											if (Arrays.asList(negatives).contains(planet.getCode())
													|| Arrays.asList(negatives).contains(skyPoint.getCode()))
												continue;
										}
									}

									boolean housable = skyPoint instanceof House;
		    		                if (acode.equals("OPPOSITION")) {
		    		                	if (planet.getCode().equals("Rakhu")
		    		                			|| planet.getCode().equals("Kethu"))
		    		                		continue;
		    		                	if (!housable)
			    		                	if (skyPoint.getCode().equals("Rakhu")
			    		                			|| skyPoint.getCode().equals("Kethu"))
			    		                		continue;
		    		                }
									String code = spa.getCode();
									List<DatePeriod> plist = periods.containsKey(code) ? periods.get(code) : new ArrayList<TransitCycleHandler.DatePeriod>();
									if (key.contains("SEPARATION")
											|| (key.contains("REPEAT") && lastdate)) {
										for (DatePeriod per : plist) {
											if (0 == per.finaldate) {
												per.finaldate = time;
												break;
											}
										}
									} else if (key.contains("EXACT")
											|| (key.contains("REPEAT") && firstdate)) {
										objects2.add(spa);
										DatePeriod period = new DatePeriod();
										period.initdate = time;
										plist.add(period);
										periods.put(code, plist);
									}
								} else if (object instanceof Planet) { //ретро
									Planet planet = (Planet)object;
									List<Object> pobjects = new ArrayList<Object>();
									pobjects.addAll(ingressList.get(Ingress._REPEAT));
									pobjects.addAll(ingressList.get(Ingress._REPEAT_HOUSE));
									for (Object object2 : pobjects) {
										SkyPointAspect spa = (SkyPointAspect)object2;
										if (!spa.getSkyPoint1().getId().equals(planet.getId()))
											continue;
										spa.setRetro(true);
										objects2.add(spa);
										String code = spa.getCode();
										List<DatePeriod> plist = periods.containsKey(code) ? periods.get(code) : new ArrayList<TransitCycleHandler.DatePeriod>();
										DatePeriod period = new DatePeriod();
										period.initdate = time;
										plist.add(period);
										periods.put(code, plist);
									}
								}
							}
							ingressmap.put(key, objects2);
						}
						dtexts.put(time, ingressmap);
						mtexts.put(m, dtexts);
						texts.put(y, mtexts);
					}
				}
			}
			years = null;
			System.out.println("Composed for: " + (System.currentTimeMillis() - run));

			//генерируем документ
			run = System.currentTimeMillis();
	        Font grayfont = PDFUtil.getAnnotationFont(false);

			DirectionService service = new DirectionService();
			DirectionAspectService servicea = new DirectionAspectService();
			PlanetTextService servicep = new PlanetTextService();

			AspectTypeService typeService = new AspectTypeService();
			AspectType positiveType = (AspectType)typeService.find(3L);

	        //года
			for (Map.Entry<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>> entry : hyears.entrySet()) {
				int y = entry.getKey();
				String syear = String.valueOf(y);
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), syear + " год", null));
				chapter.setNumberDepth(0);

				//месяцы
				Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = hyears.get(y);
				Map<Integer, Map<Long, Map<String, List<Object>>>> mtexts = texts.get(y);

				for (Map.Entry<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> entry2 : months2.entrySet()) {
					int m = entry2.getKey();
					Calendar calendar = Calendar.getInstance();
					calendar.set(y, m, 1);

					Map<Long, Map<String, List<Object>>> dtexts = mtexts.get(m);
					if (dtexts.isEmpty())
						continue;
					for (Map.Entry<Long, Map<String, List<Object>>> dentry : dtexts.entrySet()) {
						Map<String, List<Object>> imap = dentry.getValue();
						boolean empty = true;
						for (Map.Entry<String, List<Object>> daytexts : imap.entrySet()) {
							List<Object> ingresses = daytexts.getValue();
							if (!ingresses.isEmpty()) {
								for (Object object : ingresses) {
									if (object instanceof SkyPointAspect) {
										SkyPointAspect spa = (SkyPointAspect)object;
										Planet planet = (Planet)spa.getSkyPoint1();
			    		                boolean main = planet.isMain();
			    		                if (main)
			        		                continue;
			    		                else {
											empty = false;
											break;
			    		                }
									}
								}
							}
						}
						if (empty) continue;

						String shortdate = sdf.format(new Date(dentry.getKey()));
						Section daysection = PDFUtil.printSection(chapter, shortdate, null);

						for (Map.Entry<String, List<Object>> itexts : imap.entrySet()) {
							List<Object> ingresses = itexts.getValue();
							for (Object object : ingresses) {
								text = "";

								if (object instanceof SkyPointAspect) {
									SkyPointAspect spa = (SkyPointAspect)object;
									Planet planet = (Planet)spa.getSkyPoint1();
		    		                boolean repeat = itexts.getKey().contains("REPEAT");
									SkyPoint skyPoint = spa.getSkyPoint2();
									String acode = spa.getAspect().getCode();

									String prefix = repeat ? "Продолжается: " : "Начинается: ";
									AspectType type = spa.getAspect().getType();
									String typeColor = type.getFontColor();
									BaseColor color = PDFUtil.htmlColor2Base(typeColor);
									Font colorbold = new Font(baseFont, 12, Font.BOLD, color);

									String til = "";
									List<DatePeriod> plist = periods.get(spa.getCode());
									if (plist != null && !plist.isEmpty()) {
										for (DatePeriod per : plist) {
											long time = dentry.getKey();
											if (per.finaldate > 0
													&& time == per.initdate) {
												Date pdate = new Date(per.finaldate);
												til = " (до " + spf.format(pdate) + " " + y + ")";
												break;
											}
										}
									}

									if (skyPoint instanceof House) {
										House house = (House)skyPoint;
	
			    		                if (acode.equals("CONJUNCTION"))
											if (planet.getCode().equals("Selena"))
												type = positiveType;
	
										DirectionText dirText = (DirectionText)service.find(planet, house, type);
										if (dirText != null)
											text = dirText.getDescription();
										String ptext = prefix;
										if (null == dirText)
											ptext += planet.getShortName() + " " + type.getSymbol() + " " + house.getName() + "<>";
										else
											ptext += term ? planet.getName() + " " + type.getSymbol() + " " + house.getDesignation() + " дом" : house.getName();

					    				daysection.addSection(new Paragraph(ptext + til, colorbold));

									} else if (skyPoint instanceof Planet) {
										long aspectid = 0;
										boolean checktype = false;
										Planet planet2 = (Planet)skyPoint;
										boolean revolution = planet.getId().equals(planet2.getId());

										PlanetAspectText dirText = (PlanetAspectText)servicea.find(spa, aspectid, checktype);
										if (dirText != null)
											text = dirText.getDescription();
										String ptext = prefix;
										if (null == dirText) {
											ptext += planet.getShortName() + "<>";
											if (!revolution)
												ptext += " " + type.getSymbol() + " " + planet2.getShortName() + "<>";
										} else {
											ptext += term ? planet.getName() : planet.getShortName();
											if (!revolution)
												ptext += " " + type.getSymbol() + " " + (term ? planet2.getName() : planet2.getShortName());
										}
										daysection.addSection(new Paragraph(ptext + til, colorbold));
									}
	
									if (text != null) {
										String descr = text;
										p = new Paragraph();
										p.add(new Chunk(descr, new Font(baseFont, 12, Font.NORMAL, color)));
										daysection.add(p);
									}
									if (spa.isRetro() && term) {
										if (planet.isFictitious()) //15.6.5. четверг, 21 июня 2035 15.12.4. пятница, 14 декабря 2035
											continue;
										if (!type.getCode().contains("POSITIVE")) {
											Phrase phrase = new Phrase("В этот период " + planet.getName() + " находится в ", grayfont);
											PlanetText planetText = (PlanetText)servicep.findByPlanet(planet.getId(), "retro");
											if (planetText != null && planetText.getUrl() != null) {
										        Chunk ch = new Chunk("ретро-фазе", PDFUtil.getLinkFont());
										        ch.setAnchor(planetText.getUrl());
										        phrase.add(ch);
											} else
												phrase.add(new Chunk("ретро-фазе", grayfont));
											phrase.add(new Chunk(", поэтому длительность прогноза затянется, а описанные события ", grayfont));
											String str = acode.equals("CONJUNCTION")
												? "приобретут для вас особую важность и в будущем ещё напомнят о себе"
												: "будут носить необратимый характер";
											phrase.add(new Chunk(str, grayfont));
											daysection.add(new Paragraph(phrase));
										}
									}
								}
								daysection.add(Chunk.NEWLINE);
							}
						}
					}
//					texts = null;
			        chapter.add(Chunk.NEXTPAGE);
				}
				doc.add(chapter);
			}
			hyears = null;
	        doc.add(PDFUtil.printCopyright());

	        long time = System.currentTimeMillis();
			System.out.println("Finished for: " + (time - run));
			System.out.println("Duration: " + (time - duration));
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
	        doc.close();
		}
	}

	private class DatePeriod {
		long initdate;
		long finaldate;

		@Override
		public String toString() {
			return initdate + " - " + finaldate;
		}
	}
}
