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
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.ChapterAutoNumber;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.ListItem;
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
import kz.zvezdochet.bean.Sign;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.OsUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.export.bean.Bar;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;

/**
 * Генерация прогноза за указанный период по месяцам
 * @author Natalie Didenko
 */
public class TransitSaveHandler extends Handler {
	private BaseFont baseFont;

	public TransitSaveHandler() {
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
			double zone = periodPart.getZone();
			Map<Long, House> houses = person.getHouses();

			int choice = DialogUtil.alertQuestion("Вопрос", "Выберите тип прогноза:", new String[] {"Реалистичный", "Оптимистичный"});
			boolean optimistic = choice > 0;

			choice = DialogUtil.alertQuestion("Вопрос", "Выберите тип прогноза:", new String[] {"Только важное", "Полный"});
			boolean important = (0 == choice);

			//Признак использования астрологических терминов
			boolean term = periodPart.isTerm();
			updateStatus("Расчёт транзитов на период", false);

			Date initDate = periodPart.getInitialDate();
			Date finalDate = periodPart.getFinalDate();
			Calendar start = Calendar.getInstance();
			start.setTime(initDate);
			Calendar end = Calendar.getInstance();
			end.setTime(finalDate);
			end.add(Calendar.DATE, 1);
			long timems = finalDate.getTime() - initDate.getTime();
			boolean longterm = (timems / 1000 > 2592000);

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/daily.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler());
	        doc.open();

	    	Font font = PDFUtil.getRegularFont();
	    	Font afont = PDFUtil.getHeaderAstroFont();

	        //metadata
	        PDFUtil.getMetaData(doc, "Ежедневный прогноз");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Ежедневный прогноз");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Ежедневный прогноз", null);
			chapter.add(p);

			String text = (person.isCelebrity() ? person.getName() : person.getCallname());
			text += ", прогноз на период: ";
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
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
			text += ", " + (important ? "только важное" : "полный");
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			if (!person.isRectified())
				p.add(new Chunk(" (не ректифицировано)", PDFUtil.getDangerFont()));
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

	        if (longterm) {
	        	chapter.add(new Paragraph("Файл содержит большой объём информации, и если прогноз рассчитан на несколько месяцев, нет смысла пытаться его весь прочитать. "
					+ "Используйте прогноз в начале каждой недели как путеводитель, помогающий понять тенденции и учесть риски.", font));
				chapter.add(Chunk.NEWLINE);
	        }
			Font red = PDFUtil.getDangerFont();
			p = new Paragraph();
			String divergence = person.isRectified() ? "1 день" : "2 дня";
			p.add(new Chunk("Общая погрешность прогноза составляет ±" + divergence + ". ", red));
			p.add(new Chunk("Это значит, что описанное событие может произойти на день раньше, если длительность прогноза составляет более одного дня (в толковании вы это увидите). ", font));
	        chunk = new Chunk("Пояснения к прогнозу", new Font(baseFont, 12, Font.UNDERLINE, PDFUtil.FONTCOLOR));
	        chunk.setAnchor("https://zvezdochet.guru/post/223/poyasnenie-k-ezhednevnym-prognozam");
	        p.add(chunk);
			chapter.add(p);
			chapter.add(Chunk.NEWLINE);

	        if (!pdefault) {
	        	divergence = person.isRectified() ? "2 дня" : "3 дня";
				chapter.add(new Paragraph("Прогноз сделан для локации «" + place.getName() + "». "
					+ "Если в течение прогнозного периода вы переедете в более отдалённое место (в другой часовой пояс или с ощутимой сменой географической широты), "
					+ "то погрешность некоторых прогнозов может составить ±" + divergence + ".", font));
				chapter.add(Chunk.NEWLINE);
	        }

			Font bold = new Font(baseFont, 12, Font.BOLD);
			chapter.add(new Paragraph("Длительность прогнозов", bold));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Если длительность прогноза не указана, значит он рассчитан на конкретную дату.", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Если длительность прогноза исчисляется днями, неделями и месяцами, то это не значит, что каждый день будет что-то происходить. "
	        	+ "Просто вероятность описанных событий будет сохраняться в течение всего периода. "
	        	+ "Чаще всего прогноз ярко проявляет себя в первый же день периода, но может сбыться и позже.", font));
	        list.add(li);
	        chapter.add(list);
			chapter.add(Chunk.NEWLINE);

			Font fonth5 = PDFUtil.getHeaderFont();
			chapter.add(new Paragraph("Диаграммы", fonth5));
			chapter.add(new Paragraph("Диаграммы показывают динамику событий в трёх категориях: позитив, негатив и важное. "
				+ "По ним видно, в какие месяцы станут актуальны те или иные сферы вашей жизни, "
				+ "и какие дни наиболее благоприятны для ваших планов.", font));
			chapter.add(Chunk.NEWLINE);

			chapter.add(new Paragraph("Позитив и негатив:", bold));
			list = new com.itextpdf.text.List(false, false, 10);
			li = new ListItem();
	        li.add(new Chunk("«Позитив» – это хороший эмоциональный настрой и благоприятные возможности, которые надо использовать по максимуму.", PDFUtil.getSuccessFont()));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("«Негатив» – это напряжение и отсутствие удачи, к которым надо быть готовым. Выработайте тактику решения проблемы и не предпринимайте рисковых действий.", red));
	        list.add(li);
	        chapter.add(list);
			chapter.add(Chunk.NEWLINE);
			
			chapter.add(new Paragraph("Важное:", bold));
			list = new com.itextpdf.text.List(false, false, 10);
			li = new ListItem();
	        li.add(new Chunk("«Важное» – это самая важная категория, указывающая на то, что произойдёт действительно что-то значительное.", font));
	        list.add(li);

	        li = new ListItem();
	        li.add(new Chunk("«Важное» сильнее всего влияет на ваше поведение в указанный период, особенно в сочетании с «Позитивом» и «Негативом».", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Если рядом с «Важным» отсутствует «Позитив», значит решение нужно принимать обдуманно, "
	        	+ "т.к. оно окажется значимым для вас и ваших близких и может иметь непредвиденные последствия.", red));
	        list.add(li);
	        chapter.add(list);

	        if (!term) {
	        	chapter.add(Chunk.NEXTPAGE);
				chapter.add(new Paragraph("Поиск по сферам жизни", fonth5));
				list = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
		        li.add(new Chunk("В начале каждого месяца есть диаграмма, показывающая, какие сферы жизни будут успешными и проблемными в этом месяце.", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("В конце каждого года приведены диаграммы сфер жизни по месяцам. Там показана динамика хороших и плохих тенденций.", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("Если понадобится что-то запланировать, то можно забить в поиск по файлу ключевое слово и посмотреть связанный с этим прогноз. " +
		        	"При этом вылезут даты, где зелёным цветом отмечены благоприятные прогнозы, красным - неблагоприятные, чёрным - нейтральные и важные дни.", font));
		        list.add(li);
				chapter.add(list);

				chapter.add(Chunk.NEWLINE);
				chapter.add(new Paragraph("Какие ключевые слова использовать:", bold));
				list = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
				String s = "Всё, что касается самореализации, обычно описывается в разделах:";
				long[] hids = {144,149,166,156,169,170,171,161};
				int j = 0;
				for (long hid : hids) {
					if (++j > 1)
						s += ",";
					s += " ";
					House house = houses.get(hid);
					s += term ? house.getDesignation() : house.getName();
				}
		        li.add(new Chunk(s, font));
		        list.add(li);
		        	
				li = new ListItem();
				s = "Всё, что касается благосостояния, описано в разделах, которые озаглавлены как:";
				hids = new long[] {145,165,151,146,147,148};
				j = 0;
				for (long hid : hids) {
					if (++j > 1)
						s += ",";
					s += " ";
					House house = houses.get(hid);
					s += term ? house.getDesignation() : house.getName();
				}
		        li.add(new Chunk(s, font));
		        list.add(li);
		        	 
				li = new ListItem();
				s = "Всё, что касается семьи, описано в разделах:";
				hids = new long[] {152,156,150};
				j = 0;
				for (long hid : hids) {
					if (++j > 1)
						s += ",";
					s += " ";
					House house = houses.get(hid);
					s += term ? house.getDesignation() : house.getName();
				}
		        li.add(new Chunk(s, font));
		        list.add(li);
		        	 
				li = new ListItem();
				s = "Всё, что касается отдыха, отпуска, праздников, описано в разделе:";
				hids = new long[] {154};
				j = 0;
				for (long hid : hids) {
					if (++j > 1)
						s += ",";
					s += " ";
					House house = houses.get(hid);
					s += term ? house.getDesignation() : house.getName();
				}
		        li.add(new Chunk(s, font));
		        list.add(li);
	
				li = new ListItem();
				s = "Всё, что касается любви, описано в разделах:";
				hids = new long[] {155,160};
				j = 0;
				for (long hid : hids) {
					if (++j > 1)
						s += ",";
					s += " ";
					House house = houses.get(hid);
					s += term ? house.getDesignation() : house.getName();
				}
		        li.add(new Chunk(s, font));
		        list.add(li);
	
				li = new ListItem();
				s = "Всё, что касается работы и трудоустройства, описано в разделах:";
				hids = new long[] {157,159,166,170};
				j = 0;
				for (long hid : hids) {
					if (++j > 1)
						s += ",";
					s += " ";
					House house = houses.get(hid);
					s += term ? house.getDesignation() : house.getName();
				}
		        li.add(new Chunk(s, font));
		        list.add(li);
	
				li = new ListItem();
				s = "Всё, что касается решения вопросов со сторонними фирмами и специалистами, описано в разделе:";
				hids = new long[] {161};
				j = 0;
				for (long hid : hids) {
					if (++j > 1)
						s += ",";
					s += " ";
					House house = houses.get(hid);
					s += term ? house.getDesignation() : house.getName();
				}
		        li.add(new Chunk(s, font));
		        list.add(li);
		        	 
				li = new ListItem();
				s = "Всё, что касается поездок и транспорта, описано в разделах:";
				hids = new long[] {148,167};
				j = 0;
				for (long hid : hids) {
					if (++j > 1)
						s += ",";
					s += " ";
					House house = houses.get(hid);
					s += term ? house.getDesignation() : house.getName();
				}
		        li.add(new Chunk(s, font));
		        list.add(li);
		        chapter.add(list);
	        }
	        doc.add(chapter);

			Map<Integer, Map<Integer, List<Long>>> years = new TreeMap<Integer, Map<Integer, List<Long>>>();
//			Map<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>> hyears = new TreeMap<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>>();
			Map<Integer, Map<Integer, Map<Long, List<TimeSeriesDataItem>>>> myears = new TreeMap<Integer, Map<Integer, Map<Long, List<TimeSeriesDataItem>>>>();
			Map<Integer, Map<Integer, Map<Long, Map<String, List<Object>>>>> texts = new TreeMap<Integer, Map<Integer, Map<Long, Map<String, List<Object>>>>>();
			Map<Integer, Map<Integer, Map<Long, Integer>>> myears2 = new TreeMap<Integer, Map<Integer, Map<Long, Integer>>>();
			Map<Integer, Map<Long, Map<Long, TreeMap<Integer, Integer>>>> hyears2 = new TreeMap<Integer, Map<Long, Map<Long, TreeMap<Integer, Integer>>>>();

			System.out.println("Prepared for: " + (System.currentTimeMillis() - run));
			run = System.currentTimeMillis();

			//разбивка дат по годам и месяцам
			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int y = calendar.get(Calendar.YEAR);
				int m = calendar.get(Calendar.MONTH);
				
				Map<Integer, List<Long>> months = years.containsKey(y) ? years.get(y) : new TreeMap<Integer, List<Long>>();
				List<Long> dates = months.containsKey(m) ? months.get(m) : new ArrayList<Long>();
				long time = date.getTime(); 
				if (!dates.contains(time))
					dates.add(time);
				Collections.sort(dates);
				months.put(m, dates);
				years.put(y, months);

//				Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = hyears.containsKey(y) ? hyears.get(y) : new TreeMap<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>();
//				months2.put(m, new TreeMap<Long, Map<Long, List<TimeSeriesDataItem>>>());
//				hyears.put(y, months2);

				Map<Integer, Map<Long, List<TimeSeriesDataItem>>> months3 = myears.containsKey(y) ? myears.get(y) : new TreeMap<Integer, Map<Long, List<TimeSeriesDataItem>>>();
				months3.put(m, new TreeMap<Long, List<TimeSeriesDataItem>>());
				myears.put(y, months3);

				Map<Integer, Map<Long, Integer>> months4 = myears2.containsKey(y) ? myears2.get(y) : new TreeMap<Integer, Map<Long, Integer>>();
				months4.put(m, new TreeMap<Long, Integer>());
				myears2.put(y, months4);

				Map<Long, Map<Long, TreeMap<Integer, Integer>>> yhouses = hyears2.containsKey(y) ? hyears2.get(y) : new TreeMap<Long, Map<Long, TreeMap<Integer, Integer>>>();
				for (House h : houses.values())
					yhouses.put(h.getId(), new TreeMap<Long, TreeMap<Integer, Integer>>());
				hyears2.put(y, yhouses);
			}

			Map<Integer, Map<String, Map<Integer,Integer>>> yitems = new TreeMap<Integer, Map<String, Map<Integer,Integer>>>();
			/**
			 * коды ингрессий, используемых в отчёте
			 */
			String[] icodes = Ingress.getKeys();

			//создаём аналогичный массив, но с домами вместо дат
			int j = -1;
			for (Map.Entry<Integer, Map<Integer, List<Long>>> entry : years.entrySet()) {
				int y = entry.getKey();
				Map<Integer, List<Long>> months = years.get(y);
				Map<Integer, Map<Long, Map<String, List<Object>>>> mtexts = texts.get(y);
				if (null == mtexts)
					mtexts = new TreeMap<Integer, Map<Long, Map<String, List<Object>>>>();

				//данные для графика года
				Map<Integer,Integer> positive = new HashMap<Integer,Integer>();
				Map<Integer,Integer> negative = new HashMap<Integer,Integer>();

				for (int i = 1; i <= 12; i++) {
					positive.put(i, 0);
					negative.put(i, 0);
				}

				Map<Integer, Map<Long, Integer>> months4 = myears2.containsKey(y) ? myears2.get(y) : new TreeMap<Integer, Map<Long, Integer>>();
				Map<Long, Map<Long, TreeMap<Integer, Integer>>> yhouses = hyears2.containsKey(y) ? hyears2.get(y) : new TreeMap<Long, Map<Long, TreeMap<Integer, Integer>>>();

				//считаем транзиты
				for (Map.Entry<Integer, List<Long>> entry2 : months.entrySet()) {
					int m = entry2.getKey();
					int month = m + 1;

					List<Long> dates = months.get(m);
					Map<Long, Map<String, List<Object>>> dtexts = mtexts.get(m);
					if (null == dtexts)
						dtexts = new TreeMap<Long, Map<String, List<Object>>>();

					Map<Long, Integer> seriesh = months4.get(m);

					for (Long time : dates) {
						++j;
						Date date = new Date(time);
						String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 12:00:00";
						Event event = new Event();
						Date edate = DateUtil.getDatabaseDateTime(sdate);
						event.setBirth(edate);
						event.setPlace(place);
						event.setZone(zone);
						event.calc(true);

						Map<String, List<Object>> ingressList = person.initIngresses(event, term);
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

							if (j > 0 && key.contains("REPEAT"))
								continue;

							List<Object> objects2 = ingressmap.containsKey(key) ? ingressmap.get(key) : new ArrayList<Object>();
							String[] negatives = {"Kethu", "Lilith"};
							for (Object object : objects) {
								if (object instanceof SkyPointAspect) {
									SkyPointAspect spa = (SkyPointAspect)object;
									String acode = spa.getAspect().getCode();
									Planet planet = (Planet)spa.getSkyPoint1();
									SkyPoint skyPoint = spa.getSkyPoint2();

									if (optimistic) {
										if (2 == spa.getAspect().getTypeid())
											continue;

										if (acode.equals("CONJUNCTION")) {
											if (Arrays.asList(negatives).contains(planet.getCode())
													|| Arrays.asList(negatives).contains(skyPoint.getCode()))
												continue;
										}
									}

									if (key.contains("SEPARATION") && planet.isFictitious())
										continue;

									boolean housable = skyPoint instanceof House;
		    		                if (acode.equals("OPPOSITION")) {
		    		                	if (planet.getCode().equals("Rakhu")
		    		                			|| planet.getCode().equals("Kethu"))
		    		                		continue;

		    		                	if (housable) {
		    		                		if (!spa.isRetro()
		    		                				&& (planet.isMain() || planet.isFictitious()))
											continue;
		    		                	} else if (skyPoint.getCode().equals("Rakhu")
			    		                		|| skyPoint.getCode().equals("Kethu"))
			    		                	continue;
		    		                }
									objects2.add(spa);

								} else if (object instanceof Planet) { //ретро или директ
									Planet planet = (Planet)object;
								    List<SkyPointAspect> transits = new ArrayList<SkyPointAspect>();
									List<Object> pobjects = new ArrayList<Object>();
									pobjects.addAll(ingressList.get(Ingress._REPEAT));
									pobjects.addAll(ingressList.get(Ingress._REPEAT_HOUSE));

									for (Object object2 : pobjects) {
										SkyPointAspect spa = (SkyPointAspect)object2;
										if (!spa.getSkyPoint1().getId().equals(planet.getId()))
											continue;
										transits.add(spa);
									}
									planet.setData(transits);
									objects2.add(planet);
								}
							}
							ingressmap.put(key, objects2);
						}
						dtexts.put(time, ingressmap);
						mtexts.put(m, dtexts);
						texts.put(y, mtexts);

						Map<Long, Map<Long, Integer>> hitems = new HashMap<Long, Map<Long, Integer>>();
						Map<Long, Integer> mitems = new HashMap<Long, Integer>();
						String[] negatives = {"Kethu", "Lilith"};

						for (Map.Entry<String, List<Object>> ientry : ingressList.entrySet()) {
							if (ientry.getKey().contains("SEPARATION"))
								continue;
							List<Object> ingresses = ientry.getValue();

							for (Object object : ingresses) {
								if (!(object instanceof SkyPointAspect))
									continue;
								SkyPointAspect spa = (SkyPointAspect)object;
								SkyPoint skyPoint = spa.getSkyPoint1();
								SkyPoint skyPoint2 = spa.getSkyPoint2();

								String code = spa.getAspect().getType().getCode();
								if (code.equals("NEUTRAL")) {
									if (skyPoint.getCode().equals("Lilith") || skyPoint.getCode().equals("Kethu")
											|| skyPoint2.getCode().equals("Lilith") || skyPoint2.getCode().equals("Kethu"))
										negative.put(month, negative.get(month) + 1);
									else
										positive.put(month, positive.get(month) + 1);
								} else if (code.equals("POSITIVE"))
									positive.put(month, positive.get(month) + 1);
								else if (code.equals("NEGATIVE"))
									negative.put(month, negative.get(month) + 1);

								//данные для диаграммы месяца
								long aid = code.equals("NEUTRAL")
										&& (skyPoint.getCode().equals("Lilith") || skyPoint.getCode().equals("Kethu"))
									? 2 : spa.getAspect().getTypeid();

								int val = mitems.containsKey(aid) ? mitems.get(aid) : 0;
								mitems.put(aid, val + 1);

								if (skyPoint2 instanceof House) {
									//данные для диаграммы домов месяца
									long id = skyPoint2.getId();
									Map<Long, Integer> amap = hitems.containsKey(id) ? hitems.get(id) : new HashMap<Long, Integer>();
									val = amap.containsKey(aid) ? amap.get(aid) : 0;
									amap.put(aid, val + 1);
									hitems.put(id, amap);

									//данные для диаграммы сфер жизни месяца
									if (skyPoint.getCode().equals("Moon"))
										continue;
									String tcode = spa.getAspect().getType().getCode();
									int point = 0;
									if (tcode.equals("NEUTRAL")) {
										if (Arrays.asList(negatives).contains(skyPoint.getCode()))
											--point;
										else
											++point;
									} else if (tcode.equals("POSITIVE"))
										++point;
									else if (tcode.equals("NEGATIVE"))
										--point;

									val = seriesh.containsKey(id) ? seriesh.get(id) : 0;
									seriesh.put(id, val + point);

									//данные для графиков домов года
									Map<Long, TreeMap<Integer, Integer>> htypes = yhouses.containsKey(id) ? yhouses.get(id) : new TreeMap<Long, TreeMap<Integer, Integer>>();
									TreeMap<Integer, Integer> hmonths = htypes.containsKey(aid) ? htypes.get(aid) : new TreeMap<Integer, Integer>();
									val = hmonths.containsKey(m) ? hmonths.get(m) : 0;
									hmonths.put(m, val + 1);
									htypes.put(aid, hmonths);
									yhouses.put(id, htypes);
								}
							}
						}
//						Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = hyears.containsKey(y) ? hyears.get(y) : new TreeMap<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>();
//						Map<Long, Map<Long, List<TimeSeriesDataItem>>> items = months2.containsKey(m) ? months2.get(m) : new TreeMap<Long, Map<Long, List<TimeSeriesDataItem>>>();
//						for (Map.Entry<Long, Map<Long, Integer>> entryh : hitems.entrySet()) {
//							Map<Long, Integer> amap = entryh.getValue();
//							long hid = entryh.getKey();
//							Map<Long, List<TimeSeriesDataItem>> map = items.containsKey(hid) ? items.get(hid) : new HashMap<Long, List<TimeSeriesDataItem>>();
//							for (Map.Entry<Long, Integer> entry3 : amap.entrySet()) {
//								long aid = entry3.getKey();
//								List<TimeSeriesDataItem> series = map.containsKey(aid) ? map.get(aid) : new ArrayList<TimeSeriesDataItem>();
//								TimeSeriesDataItem tsdi = new TimeSeriesDataItem(new Day(date), entry3.getValue());
//								if (!series.contains(tsdi))
//									series.add(tsdi);
//								map.put(aid, series);
//								items.put(hid, map);
//							}
//						}
//						months2.put(m, items);

						Map<Integer, Map<Long, List<TimeSeriesDataItem>>> months3 = myears.containsKey(y) ? myears.get(y) : new TreeMap<Integer, Map<Long, List<TimeSeriesDataItem>>>();
						Map<Long, List<TimeSeriesDataItem>> map = months3.containsKey(m) ? months3.get(m) : new TreeMap<Long, List<TimeSeriesDataItem>>();
						for (Map.Entry<Long, Integer> entry3 : mitems.entrySet()) {
							long aid = entry3.getKey();
							List<TimeSeriesDataItem> series = map.containsKey(aid) ? map.get(aid) : new ArrayList<TimeSeriesDataItem>();
							TimeSeriesDataItem tsdi = new TimeSeriesDataItem(new Day(date), entry3.getValue());
							if (!series.contains(tsdi))
								series.add(tsdi);
							map.put(aid, series);
						}
						months3.put(m, map);
					}
					months4.put(m, seriesh);
					hyears2.put(y, yhouses);
				}
				Map<String, Map<Integer,Integer>> ymap = new HashMap<String, Map<Integer,Integer>>();
				ymap.put("positive", positive);
				ymap.put("negative", negative);
				yitems.put(y, ymap);
			}
			years = null;
			System.out.println("Composed for: " + (System.currentTimeMillis() - run));

			//генерируем документ
			run = System.currentTimeMillis();
	        Font hfont = new Font(baseFont, 16, Font.BOLD, PDFUtil.FONTCOLOR);
	        Font grayfont = PDFUtil.getAnnotationFont(false);
        	Font fonta = PDFUtil.getLinkFont();

			DirectionService service = new DirectionService();
			DirectionAspectService servicea = new DirectionAspectService();
			PlanetTextService servicep = new PlanetTextService();

			String[] pnegative = {"Lilith", "Kethu"};

	        //года
			for (Map.Entry<Integer, Map<Integer, Map<Long, Map<String, List<Object>>>>> entry : texts.entrySet()) {
				int y = entry.getKey();
				String syear = String.valueOf(y);
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), syear + " год", null));
				chapter.setNumberDepth(0);

				//диаграмма года
				Section section = PDFUtil.printSection(chapter, "Соотношение событий " + syear + " года", null);
				Map<String, Map<Integer,Integer>> ymap = yitems.get(y);
				Map<Integer,Integer> positive = ymap.get("positive");
				Map<Integer,Integer> negative = ymap.get("negative");

				Bar[] bars = new Bar[24];
				for (int i = 0; i < 12; i++) {
					int month = i + 1;
					String smonth = DateUtil.getMonthName(month).substring(0, 3);
					bars[i] = new Bar(smonth, positive.get(month), null, "Позитивные события", null);
					bars[i + 12] = new Bar(smonth, negative.get(month) * (-1), null, "Негативные события", null);
				}
				Image image = PDFUtil.printStackChart(writer, "Месяцы года", "Возраст", "Количество", bars, 500, 400, true);
				section.add(image);

				p = new Paragraph();
	 			p.add(new Chunk("Диаграммы сфер жизни по месяцам приведены ", font));
	        	Anchor anchor = new Anchor("ниже", fonta);
	        	anchor.setReference("#charts" + y);
	 	        p.add(anchor);
	 			section.add(p);
				section.add(Chunk.NEXTPAGE);

//				Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = hyears.get(y);
				Map<Integer, Map<Long, List<TimeSeriesDataItem>>> months3 = myears.get(y);
				Map<Integer, Map<Long, Map<String, List<Object>>>> mtexts = texts.get(y);
				Map<Integer, Map<Long, Integer>> months4 = myears2.get(y);

				//месяцы
				for (Map.Entry<Integer, Map<Long, Map<String, List<Object>>>> entry2 : mtexts.entrySet()) {
					int m = entry2.getKey();
					Calendar calendar = Calendar.getInstance();
					calendar.set(y, m, 1);
					String ym = new SimpleDateFormat("LLLL").format(calendar.getTime()) + " " + y;
					section = PDFUtil.printSection(chapter, ym, null);

					//график месяца
					Map<Long, List<TimeSeriesDataItem>> mitems = months3.get(m);
					TimeSeriesCollection dataset = new TimeSeriesCollection();
					for (Map.Entry<Long, List<TimeSeriesDataItem>> entry3 : mitems.entrySet()) {
		        		List<TimeSeriesDataItem> series = entry3.getValue();
		        		if (null == series || series.isEmpty())
		        			continue;
		        		Long aid = entry3.getKey();
		        		TimeSeries timeSeries = new TimeSeries(aid < 2 ? "Важное" : (aid < 3 ? "Негатив" : "Позитив"));
						for (TimeSeriesDataItem tsdi : series)
							timeSeries.add(tsdi);
						dataset.addSeries(timeSeries);
		        	}
		        	if (dataset.getSeriesCount() > 0) {
					    image = PDFUtil.printTimeChart(writer, "Благоприятные и неблагоприятные дни", "", "Баллы", dataset, 500, 0, true);
						section.add(image);
						section.add(Chunk.NEWLINE);
					}
					section.add(Chunk.NEWLINE);

					//диаграмма месяца
					printDiagramDescr(section, font);
					Map<Long, Integer> seriesh = months4.get(m);
					List<Bar> ditems = new ArrayList<Bar>();
					for (Map.Entry<Long, Integer> entry3 : seriesh.entrySet()) {
						int val = entry3.getValue();
						if (Math.abs(val) < 2)
							continue;
						House house = houses.get(entry3.getKey());
						Bar bar = new Bar();
				    	bar.setName(house.getName());
					    bar.setValue(val);
						bar.setColor(house.getColor());
						bar.setCategory(ym);
						ditems.add(bar);
					}
					bars = new Bar[ditems.size()];
					ditems.toArray(bars);
					section.add(PDFUtil.printBars(writer, "", null, "Сферы жизни", "Баллы", bars, 500, 300, false, false, false));
					section.add(new Paragraph("Далее приведён прогноз по этим сферам жизни", font));
					section.add(Chunk.NEWLINE);

					Map<Long, Map<String, List<Object>>> dtexts = mtexts.get(m);
					if (dtexts.isEmpty())
						continue;
					for (Map.Entry<Long, Map<String, List<Object>>> dentry : dtexts.entrySet()) {
						Map<String, List<Object>> imap = dentry.getValue();
						boolean empty = true;
						for (Map.Entry<String, List<Object>> daytexts : imap.entrySet()) {
							if (!empty)
								break;
							String key = daytexts.getKey();
    		                boolean repeat = key.contains("REPEAT");
    		                if (repeat)
        		                continue;
							List<Object> ingresses = daytexts.getValue();
							if (!ingresses.isEmpty()) {
								for (Object object : ingresses) {
									if (object instanceof SkyPointAspect) {
										SkyPointAspect spa = (SkyPointAspect)object;
										Planet planet = (Planet)spa.getSkyPoint1();
			    		                boolean main = planet.isMain();
			    		                boolean separation = key.contains("SEPARATION");
			    		                if (main && separation) {
			        		                continue;
			    		                }
//			    		                if (important) {
//				    		                if (planet.getCode().equals("Moon")
//				    		                		&& !spa.getAspect().getCode().equals("CONJUNCTION"))
//		    		                			continue;
//			    		                }
			    		                if (planet.getCode().equals("Moon")) {
		    		                		if (spa.getSkyPoint2() instanceof House
		    		                				&& !spa.getAspect().getCode().equals("CONJUNCTION"))
		    		                			continue;
			    		                } else {
											empty = false;
											break;
			    		                }
									}
								}
							}
						}
						if (empty) continue;

						String shortdate = sdf.format(new Date(dentry.getKey()));
						Section daysection = PDFUtil.printSubsection(section, shortdate, null);

						for (Map.Entry<String, List<Object>> itexts : imap.entrySet()) {
	    		            boolean main = false;

							List<Object> ingresses = itexts.getValue();
							for (Object object : ingresses) {
								text = "";
								String code = "";

								if (object instanceof SkyPointAspect) {
									SkyPointAspect spa = (SkyPointAspect)object;
									Planet planet = (Planet)spa.getSkyPoint1();
									
									if (important) {
			    		                if (planet.getCode().equals("Moon")
			    		                		&& !spa.getAspect().getCode().equals("CONJUNCTION"))
	    		                			continue;
		    		                }

		    		                main = planet.isMain();
		    		                boolean exact = itexts.getKey().contains("EXACT");
		    		                boolean separation = itexts.getKey().contains("SEPARATION");
		    		                boolean repeat = itexts.getKey().contains("REPEAT");
		    		                if (main && (separation || repeat))
		        		                continue;

									SkyPoint skyPoint = spa.getSkyPoint2();
									String acode = spa.getAspect().getCode();
			    		            String rduration = spa.isRetro() ? " и более" : "";

									String prefix = "";
									if (!main) {
		               	                if (exact)
		               	                	prefix = "Начинается: ";
										else if (repeat)
											prefix = "Продолжается: ";
										else if (separation)
											prefix = "Заканчивается: ";
									}
/*
 * 			Anchor anchor = new Anchor("Рисунок вашего гороскопа", fonta);
            anchor.setReference("#cosmogram");
			Paragraph p = new Paragraph();
	        p.add(anchor);
			p.add(new Chunk(" показывает общую картину, которая не в деталях, а глобально описывает ваше предназначение и опыт прошлого:", font));
			section.add(p);
			
        	Anchor anchorTarget = new Anchor(title, fonth3);
        	anchorTarget.setName(anchor);
        	p.add(anchorTarget);

 */
									AspectType type = spa.getAspect().getType();
									String typeColor = type.getFontColor();
									BaseColor color = PDFUtil.htmlColor2Base(typeColor);
									Font colorbold = new Font(baseFont, 12, Font.BOLD, color);
				    				String tduration = separation ? "" : spa.getTransitDuration();

									if (skyPoint instanceof House) {
										House house = (House)skyPoint;
	
			    		                if (!acode.equals("CONJUNCTION")
												&& planet.getCode().equals("Moon"))
											continue;
	
										DirectionText dirText = (DirectionText)service.find(planet, house, type);
										if (dirText != null) {
											text = spa.isRetro() && !planet.isFictitious() ? dirText.getRetro() : dirText.getDescription();
											code = dirText.getCode();
										}
										String ptext = prefix;
										if (null == dirText
												|| ((!separation && !repeat) && null == dirText.getDescription())
												|| ((separation || repeat) && (null == code || code.isEmpty()))) {
											ptext += planet.getShortName() + " " + type.getSymbol() + " " + house.getName() + "<>";
										} else if (repeat)
											ptext += term ? planet.getName() + " " + type.getSymbol() + " " + house.getDesignation() + " дом" : code;
										else if (!separation)
											ptext += term ? planet.getName() + " " + type.getSymbol() + " " + house.getDesignation() + " дом" : house.getName();

					    				daysection.addSection(new Paragraph(ptext, colorbold));
					    				if (term) {
											String pretext = spa.getAspect().getCode().equals("CONJUNCTION")
												? (null == house.getGeneral() ? "с куспидом" : "с вершиной")
												: (null == house.getGeneral() ? "к куспиду" : "к вершине");

											p = new Paragraph();
											p.add(new Chunk(spa.getAspect().getName() + " транзитной планеты ", grayfont));
											p.add(new Chunk(planet.getSymbol(), afont));
											p.add(new Chunk(" " + planet.getName(), grayfont));
											p.add(new Chunk(" из " + CalcUtil.roundTo(planet.getLongitude(), 2) + "° (", grayfont));
											Sign sign = planet.getSign();
						    				p.add(new Chunk(sign.getSymbol(), afont));
						    				p.add(new Chunk(" " + sign.getName(), grayfont));
						    				String mark = planet.getMark("sign", term);
						    				p.add(new Chunk((mark.isEmpty() ? "" : " " + mark) + ", ", grayfont));
						    				House house2 = planet.getHouse();
											p.add(new Chunk(house2.getDesignation() + " дом, сектор «" + house2.getName() + "»", grayfont));
											mark = planet.getMark("house", term);
						    				p.add(new Chunk((mark.isEmpty() ? "" : " " + mark) + ") ", grayfont));
											p.add(new Chunk(pretext + " " + house.getDesignation() + " дома", grayfont));
						    				if (!acode.equals("CONJUNCTION"))
												p.add(new Chunk(" (сектор «" + house.getName() + "»)", grayfont));
											daysection.add(p);
					    				} else if (tduration.length() > 0 && !repeat)
						    				daysection.add(new Paragraph("Длительность прогноза: " + tduration + rduration, grayfont));

									} else if (skyPoint instanceof Planet) {
										long aspectid = 0;
										boolean checktype = false;
										Planet planet2 = (Planet)skyPoint;
										boolean revolution = planet.getId().equals(planet2.getId());
										if (planet.getCode().equals("Moon")
												&& !acode.equals("CONJUNCTION"))
								            aspectid = spa.getAspect().getId();

										PlanetAspectText dirText = (PlanetAspectText)servicea.find(spa, aspectid, checktype);
										if (dirText != null) {
											text = dirText.getDescription();
											code = dirText.getCode();
										}
										String ptext = prefix;
										if (null == dirText
												|| ((!separation && !repeat) && null == dirText.getDescription())
												|| ((separation || repeat) && (null == code || code.isEmpty()))) {
											ptext += planet.getShortName() + "<>";
											if (!revolution)
												ptext += " " + type.getSymbol() + " " + planet2.getShortName() + "<>";
										} else if (repeat) {
											if (term) {
												ptext += planet.getName();
												if (!revolution)
													ptext += " " + type.getSymbol() + " " + planet2.getName();
											} else
												ptext += code;
										} else if (!separation) {
											boolean bad = type.getPoints() < 0
												|| (type.getCode().equals("NEUTRAL")
													&& (Arrays.asList(pnegative).contains(planet.getCode())
														|| Arrays.asList(pnegative).contains(skyPoint.getCode())));
						    				String pname = bad ? planet.getAspectingBadName() : planet.getAspectingName();
						    				String pname2 = bad ? planet2.getAspectedBadName() : planet2.getAspectedName();
											ptext += term ? planet.getName() : pname;
											if (!revolution)
												ptext += " " + type.getSymbol() + " " + (term ? planet2.getName() : pname2);
										}
										daysection.addSection(new Paragraph(ptext, colorbold));
										if (term) {
											String pretext = spa.getAspect().getCode().equals("CONJUNCTION")
												? "с натальной планетой"
												: "к натальной планете";

						    				p = new Paragraph();
						    				if (dirText != null)
						    					p.add(new Chunk(dirText.getMark() + " ", grayfont));
								    		p.add(new Chunk(spa.getAspect().getName() + " транзитной планеты ", grayfont));
						    				p.add(new Chunk(planet.getSymbol(), afont));
						    				p.add(new Chunk(" " + planet.getName(), grayfont));
											p.add(new Chunk(" из " + CalcUtil.roundTo(planet.getLongitude(), 2) + "° (", grayfont));
											Sign sign = planet.getSign();
						    				p.add(new Chunk(sign.getSymbol(), afont));
						    				p.add(new Chunk(" " + sign.getName() + ", ", grayfont));
						    				House house = planet.getHouse();
						    				p.add(new Chunk(house.getDesignation() + " дом, сектор «" + house.getName() + "») ", grayfont));
								    		p.add(new Chunk(pretext + " ", grayfont));
						    				p.add(new Chunk(planet2.getSymbol(), afont));
						    				p.add(new Chunk(" " + planet2.getName(), grayfont));
						    				if (acode.equals("CONJUNCTION")) {
												Sign sign2 = planet2.getSign();
							    				p.add(new Chunk(" (" + sign2.getSymbol(), afont));
							    				p.add(new Chunk(" " + sign2.getName() + ", ", grayfont));
							    				House house2 = planet2.getHouse();
							    				p.add(new Chunk(house2.getDesignation() + " дом, сектор «" + house2.getName() + "») ", grayfont));
						    				}
						    				daysection.add(p);
										} else if (tduration.length() > 0 && !repeat)
						    				daysection.add(new Paragraph("Длительность прогноза: " + tduration + rduration, grayfont));
									}
	
									if (text != null) {
										String descr = "";
			               	            if (main)
			               	                descr = text;
			               	            else {
			               	                if (exact)
			               	                    descr = text;
			               	            	else
			               	                    descr = repeat ? text : code;
			               	            }
										p = new Paragraph();
										if (null == descr)
											descr = "";
										p.add(new Chunk(descr, new Font(baseFont, 12, Font.NORMAL, color)));
										daysection.add(p);
									}
									if (spa.isRetro()
											&& !separation
											&& !planet.isFictitious()
											&& !type.getCode().contains("POSITIVE")) {
										Phrase phrase = new Phrase();
										if (term) {
											phrase.add(new Chunk("В этот период " + planet.getName() + " находится в ", grayfont));
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
										} else {
											phrase.add(new Chunk("Длительность прогноза затянется, а описанные события ", grayfont));
											String str = acode.equals("CONJUNCTION")
												? "приобретут для вас особую важность и в будущем ещё напомнят о себе"
												: "будут носить необратимый характер";
											phrase.add(new Chunk(str, grayfont));											
										}
										daysection.add(new Paragraph(phrase));
									}

								//изменение движения планеты
								} else if (object instanceof Planet) {
									Planet planet = (Planet)object;
				    				@SuppressWarnings("unchecked")
									List<SkyPointAspect> transits = (List<SkyPointAspect>)planet.getData();
				    				boolean notransits = (null == transits || transits.isEmpty());
				    				if (!term && notransits)
				    					continue;

									boolean motion = itexts.getKey().contains("MOTION");
									if (!motion)
										continue;
									boolean direct = itexts.getKey().equals(Ingress._DIRECT);
									String direction = direct ? "директное" : "обратное";
									String ptext = planet.getName() + " переходит в " + direction + " движение";
				    				daysection.addSection(new Paragraph(ptext, bold));

									if (notransits) {
										if (term)
											daysection.add(new Paragraph("Как-то ощутимо на вас это не повлияет", grayfont));
									} else {
										if (term) {
											Planet rp = person.getPlanets().get(planet.getId());
											if (rp.isRetrograde())
												daysection.add(new Paragraph("В момент вашего рождения планета " + planet.getName() + " двигалась в обратном направлении, поэтому для вас сегодня станут особо актуальными следующие прогнозы:", font));
											else
												daysection.add(new Paragraph("Сегодня " + planet.getName() + " меняет направление, поэтому снова станут актуальны следующие прогнозы:", font));
										} else
											daysection.add(new Paragraph("Поэтому снова станут актуальны следующие прогнозы:", grayfont));

										for (SkyPointAspect spa : transits) {
											SkyPoint skyPoint = spa.getSkyPoint2();
											AspectType type = spa.getAspect().getType();
											String acode = spa.getAspect().getCode();
											String typeColor = type.getFontColor();
											BaseColor color = PDFUtil.htmlColor2Base(typeColor);
											Font colorbold = new Font(baseFont, 12, Font.NORMAL, color);
											daysection.add(Chunk.NEWLINE);

											if (skyPoint instanceof House) {
												House house = (House)skyPoint;
			
					    		                if (planet.getCode().equals("Moon")
					    		                		&& !acode.equals("CONJUNCTION"))
													continue;
			
												DirectionText dirText = (DirectionText)service.find(planet, house, type);
												if (dirText != null)
													text = dirText.getRetro();

												ptext = (null == dirText || null == dirText.getDescription())
													? planet.getShortName() + " " + type.getSymbol() + " " + house.getName() + "<>"
													: text;

							    				if (term) {
													String pretext = spa.getAspect().getCode().equals("CONJUNCTION")
														? (null == house.getGeneral() ? "с куспидом" : "с вершиной")
														: (null == house.getGeneral() ? "к куспиду" : "к вершине");

													p = new Paragraph();
													p.add(new Chunk(planet.getMark("house", term) + " ", grayfont));
													p.add(new Chunk(spa.getAspect().getName() + " транзитной ретро-планеты ", grayfont));
													p.add(new Chunk(planet.getSymbol(), afont));
													p.add(new Chunk(" " + planet.getName(), grayfont));
													p.add(new Chunk(" из " + CalcUtil.roundTo(planet.getLongitude(), 2) + "° (", grayfont));
													Sign sign = planet.getSign();
								    				p.add(new Chunk(sign.getSymbol(), afont));
								    				p.add(new Chunk(" " + sign.getName() + ", ", grayfont));
								    				House house2 = planet.getHouse();
													p.add(new Chunk(house2.getDesignation() + " дом, сектор «" + house2.getName() + "») ", grayfont));
													p.add(new Chunk(pretext + " " + house.getDesignation() + " дома (сектор «" + house.getName() + "»)", grayfont));
													daysection.add(p);					    					
							    				}
												daysection.add(new Paragraph(ptext, colorbold));
											} else if (skyPoint instanceof Planet) {
												long aspectid = 0;
												Planet planet2 = (Planet)skyPoint;
												boolean revolution = planet.getId().equals(planet2.getId());
												if (planet.getCode().equals("Moon"))
										            aspectid = spa.getAspect().getId();
			
												PlanetAspectText dirText = (PlanetAspectText)servicea.find(spa, aspectid, false);
												if (dirText != null)
													text = dirText.getDescription();

												ptext = (null == dirText || null == dirText.getDescription())
													? planet.getShortName() + (revolution ? "" : " " + type.getSymbol() + " " + planet2.getShortName() + "<>")
													: text;

												if (term) {
													String pretext = spa.getAspect().getCode().equals("CONJUNCTION")
														? "с натальной планетой"
														: "к натальной планете";

								    				p = new Paragraph();
								    				if (dirText != null)
								    					p.add(new Chunk(dirText.getMark() + " ", grayfont));
										    		p.add(new Chunk(spa.getAspect().getName() + " транзитной ретро-планеты ", grayfont));
								    				p.add(new Chunk(planet.getSymbol(), afont));
								    				p.add(new Chunk(" " + planet.getName(), grayfont));
													p.add(new Chunk(" из " + CalcUtil.roundTo(planet.getLongitude(), 2) + "° (", grayfont));
													Sign sign = planet.getSign();
								    				p.add(new Chunk(sign.getSymbol(), afont));
								    				p.add(new Chunk(" " + sign.getName() + ", ", grayfont));
								    				House house = planet.getHouse();
								    				p.add(new Chunk(house.getDesignation() + " дом, сектор «" + house.getName() + "») ", grayfont));
										    		p.add(new Chunk(pretext + " ", grayfont));
								    				p.add(new Chunk(planet2.getSymbol(), afont));
								    				p.add(new Chunk(" " + planet2.getName(), grayfont));
													Sign sign2 = planet2.getSign();
								    				p.add(new Chunk(" (" + sign2.getSymbol(), afont));
								    				p.add(new Chunk(" " + sign2.getName() + ", ", grayfont));
								    				House house2 = planet2.getHouse();
								    				p.add(new Chunk(house2.getDesignation() + " дом, сектор «" + house2.getName() + "») ", grayfont));
								    				daysection.add(p);
												}
												daysection.add(new Paragraph(ptext, colorbold));												
											}
										}
									}
								}
								daysection.add(Chunk.NEWLINE);
							}
						}
						daysection.add(Chunk.NEXTPAGE);
					}
//					texts = null;

					//графики месяца по домам
//					section.add(Chunk.NEXTPAGE);
//					section = PDFUtil.printSubsection(section, ym + " по сферам жизни", "charts" + m + "" + y);
//
//					list = new com.itextpdf.text.List(false, false, 10);
//					li = new ListItem();
//			        li.add(new Chunk("Если график представляет собой точку, значит актуальность данной сферы ограничена одним днём,", font));
//			        list.add(li);
//
//					li = new ListItem();
//			        li.add(new Chunk("если график в виде линии, то точки на нём укажут на важные дни периода в данной сфере", font));
//			        list.add(li);
//			        section.add(list);
//			        section.add(Chunk.NEWLINE);
//
//					Map<Long, Map<Long, List<TimeSeriesDataItem>>> items = entry2.getValue();
//			        i = -1;
//					for (Map.Entry<Long, Map<Long, List<TimeSeriesDataItem>>> entryh : items.entrySet()) {
//						long houseid = entryh.getKey();
//						House house = houses.get(houseid);
//						Map<Long, List<TimeSeriesDataItem>> map = entryh.getValue();
//						if (map.isEmpty())
//							continue;
//						dataset = new TimeSeriesCollection();
//						for (Map.Entry<Long, List<TimeSeriesDataItem>> entry3 : map.entrySet()) {
//			        		List<TimeSeriesDataItem> series = entry3.getValue();
//			        		if (null == series || series.isEmpty())
//			        			continue;
//			        		Long aid = entry3.getKey();
//			        		TimeSeries timeSeries = new TimeSeries(aid < 2 ? "Важное" : (aid < 3 ? "Негатив" : "Позитив"));
//							for (TimeSeriesDataItem tsdi : series)
//								timeSeries.add(tsdi);
//							dataset.addSeries(timeSeries);
//			        	}
//			        	if (dataset.getSeriesCount() > 1) { //точечный график не отображаем
//			        		if (++i > 1) {
//			        			i = 0;
//			        			section.add(Chunk.NEXTPAGE);
//			        		}
//				        	section.addSection(new Paragraph(house.getName(), hfont));
//				        	section.add(new Paragraph(ym + ": " + house.getDescription(), font));
//						    image = PDFUtil.printTimeChart(writer, "", "", "Баллы", dataset, 500, 0, true);
//							section.add(image);
//							section.add(Chunk.NEWLINE);
//			        	}
//					}
//			        chapter.add(Chunk.NEXTPAGE);
				}
				//графики года
				if (longterm) {
					section = PDFUtil.printSection(chapter, "Диаграммы сфер жизни по месяцам " + y + " года", "charts" + y);
					Map<Long, Map<Long, TreeMap<Integer, Integer>>> yhouses = hyears2.get(y);
					int i = -1;
					for (Map.Entry<Long, Map<Long, TreeMap<Integer, Integer>>> entryh : yhouses.entrySet()) {
						long houseid = entryh.getKey();
						House house = houses.get(houseid);
						Map<Long, TreeMap<Integer, Integer>> mtypes = entryh.getValue();
						if (mtypes.isEmpty())
							continue;
						Map<String, Object[]> types = new HashMap<String, Object[]>();
			        	for (Map.Entry<Long, TreeMap<Integer, Integer>> entrya : mtypes.entrySet()) {
			        		TreeMap<Integer, Integer> atypes = entrya.getValue();
							if (atypes.isEmpty())
								continue;
	
							Long aid = entrya.getKey();
							List<Integer> names = new ArrayList<Integer>();
							List<Integer> values = new ArrayList<Integer>();
				        	for (Map.Entry<Integer, Integer> entrym : atypes.entrySet()) {
				        		int m = entrym.getKey();
				        		names.add(m + 1);
				        		values.add(entrym.getValue());
				        	}
							String name = (aid < 2 ? "Важное" : (aid < 3 ? "Негатив" : "Позитив"));
				        	types.put(name, new Object[] {names, values});
			        	}
			        	if (types.isEmpty())
			        		continue;
						section.addSection(new Paragraph(house.getName(), hfont));
			        	section.add(new Paragraph(y + ": " + house.getDescription(), font));
					    image = PDFUtil.printGraphics(writer, "", "Месяцы " + y, "Баллы", types , 500, 0, true);
						section.add(image);
						if (++i % 2 > 0)
							section.add(Chunk.NEXTPAGE);
						else
							section.add(Chunk.NEWLINE);
					}
				}
				doc.add(chapter);
			}
//			hyears = null;
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

	/**
	 * Выводим описание диаграммы возраста
	 * @param section раздел
	 * @param font шрифт
	 */
	private void printDiagramDescr(Section section, Font font) {
		if (OsUtil.getOS().equals(OsUtil.OS.LINUX)) {
			String text = "Следующая диаграмма показывает, какие сферы жизни будут актуальны в течение месяца:";
			section.add(new Paragraph(text, font));
	
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Показатели выше нуля указывают на успех и лёгкость", new Font(baseFont, 12, Font.NORMAL, new BaseColor(0, 102, 102))));
	        list.add(li);
	
			li = new ListItem();
	        li.add(new Chunk("Показатели ниже нуля указывают на трудности и напряжение", new Font(baseFont, 12, Font.NORMAL, new BaseColor(102, 0, 51))));
	        list.add(li);
	        section.add(list);
		} else {
			String text = "Следующая диаграмма показывает, какие сферы жизни будут актуальны в течение месяца:";
			section.add(new Paragraph(text, font));			
		}
	}
}
