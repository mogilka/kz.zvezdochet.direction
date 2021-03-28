package kz.zvezdochet.direction.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.Tab;
import kz.zvezdochet.core.ui.decoration.InfoDecoration;
import kz.zvezdochet.core.ui.listener.ListSelectionListener;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.util.GUIutil;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.bean.Transit;
import kz.zvezdochet.direction.provider.TransitLabelProvider;
import kz.zvezdochet.direction.service.TransitService;
import kz.zvezdochet.part.CosmogramComposite;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.part.Messages;
import kz.zvezdochet.provider.EventProposalProvider;
import kz.zvezdochet.provider.EventProposalProvider.EventContentProposal;
import kz.zvezdochet.provider.PlaceProposalProvider;
import kz.zvezdochet.provider.PlaceProposalProvider.PlaceContentProposal;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.service.EventService;

/**
 * Представление для расчёта транзита на день
 * и отображения сохранённых транзитов персоны
 * @author Natalie Didenko
 *
 */
public class EventPart extends ModelListView implements ICalculable {
	@Inject
	public EventPart() {}

	private Event person;
	private Event trevent;
	private Object transitData;
	private Place trplace;

	private Label lbName;
	private Text txName;
	private Text txPlace;
	private Text txLatitude;
	private Text txLongitude;
	private Text txZone;
	private Text txGreenwich;
	private Label lbBirth;
	private DateTime dtBirth;
	private Text txDescr;
	private TableViewer transitViewer;
	private List<Model> aspects = null;
	
	/**
	 * Режим расчёта транзитов.
	 * 1 - режим планет события в карте персоны
	 * 2 - по умолчанию отображаются планеты персоны в карте события
	 */
	private int MODE_CALC = 2;

	private CosmogramComposite cmpCosmogram;
	private CosmogramComposite cmpCosmogram2;
	private CTabFolder folder;
	private CTabFolder folder2;
	private Group grPlanets;
	private Group grHouses;
	private Group grTransits;

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}

	@Override
	protected void init(Composite parent) {
		super.init(parent);
		Group grCosmogram = new Group(group, SWT.NONE);
		grCosmogram.setText("Космограмма");
		cmpCosmogram = new CosmogramComposite(grCosmogram, SWT.NONE);

//		sashForm = new SashForm(parent, SWT.HORIZONTAL);
//		Group gr = new Group(sashForm, SWT.NONE);
//		initFilter(gr);
//		tableViewer = new TableViewer(gr, SWT.BORDER | SWT.FULL_SELECTION);
//		Table table = tableViewer.getTable();
//		table.setHeaderVisible(true);
//		table.setLinesVisible(true);
//		addColumns();

		folder2 = new CTabFolder(grCosmogram, SWT.BORDER);
		folder2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder2.setSimple(false);
		folder2.setUnselectedCloseVisible(false);
		Tab[] tabs = initTabs2();
		for (Tab tab : tabs) {
			CTabItem item = new CTabItem(folder2, SWT.CLOSE);
			item.setText(tab.name);
			item.setImage(tab.image);
			item.setControl(tab.control);
		}
		folder2.pack();

		folder = new CTabFolder(grCosmogram, SWT.BORDER);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.setSimple(false);
		folder.setUnselectedCloseVisible(false);
		tabs = initTabs();
		for (Tab tab : tabs) {
			CTabItem item = new CTabItem(folder, SWT.CLOSE);
			item.setText(tab.name);
			item.setImage(tab.image);
			item.setControl(tab.control);
		}
		folder.pack();
		GridLayoutFactory.swtDefaults().applyTo(grCosmogram);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grCosmogram);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).
			hint(514, 514).span(3, 1).grab(true, false).applyTo(cmpCosmogram);
	}

	@Override
	protected String[] initTableColumns() {
		return new String[] {
			"Имя",
			"Дата" };
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Event event = (Event)element;
				switch (columnIndex) {
					case 0: return event.getName();
					case 1: return DateUtil.formatDateTime(event.getBirth());
				}
				return null;
			}
		};
	}

	/**
	 * Перерисовка космограммы
	 * @param event событие|персона
	 * @param event2 событие|персона
	 */
	private void refreshCard(Event event, Event event2) {
		Map<String, Object> params = new HashMap<>();
		params.put("exact", true);
		params.put("houseAspectable", true);

		String[] atcodes = {"NEUTRAL", "POSITIVE", "NEGATIVE"};
		List<String> aparams = new ArrayList<String>();
		aparams.addAll(Arrays.asList(atcodes));
		params.put("aspects", aparams);

		cmpCosmogram.paint(event, event2, params);
		cmpCosmogram2.paint(event2, null, params);
	}

	/**
	 * Инициализация персоны
	 * @param event персона
	 */
	public void setPerson(Event event) {
		try {
			person = event;
			setData(new TransitService().findTransits(event.getId()));
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Инициализация вкладок космограммы
	 * @return массив вкладок
	 */
	private Tab[] initTabs() {
		Tab[] tabs = new Tab[4];
		//настройки расчёта
		Tab tab = new Tab();
		tab.name = "Настройки";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.runner", "icons/configure.gif").createImage();
		Group group = new Group(folder, SWT.NONE);
		group.setText("Общие");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tab.control = group;
		tabs[0] = tab;

		//планеты
		tab = new Tab();
		tab.name = "Планеты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/planet.gif").createImage();
		grPlanets = new Group(folder, SWT.NONE);
		String[] titles = { "Планета", "Координата #1", "Координата #2"	};
		Table table = new Table(grPlanets, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grPlanets.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (String title : titles) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(title.toString());
		}	
		tab.control = grPlanets;
		GridLayoutFactory.swtDefaults().applyTo(grPlanets);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grPlanets);
		tabs[1] = tab;
		
		//дома
		tab = new Tab();
		tab.name = "Дома";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/home.gif").createImage();
		grHouses = new Group(folder, SWT.NONE);
		titles = new String[] {"Дом", "Координата #1", "Координата #2"};
		table = new Table(grHouses, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grHouses.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText(titles[i]);
		}
		tab.control = grHouses;
		GridLayoutFactory.swtDefaults().applyTo(grHouses);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grHouses);
		tabs[2] = tab;

		//транзиты
		tab = new Tab();
		tab.name = "Транзиты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.direction", "icons/transit.gif").createImage();
		grTransits = new Group(folder, SWT.NONE);
		
		transitViewer = new TableViewer(grTransits, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		table = transitViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);

		titles = getTableColumns();
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText(titles[i].toString());
		}
		transitViewer.setContentProvider(new ArrayContentProvider());
		transitViewer.setLabelProvider(new TransitLabelProvider());

		tab.control = grTransits;
		GridLayoutFactory.swtDefaults().applyTo(grTransits);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grTransits);
		tabs[3] = tab;

		return tabs;
	}

	/**
	 * Обновление вкладок
	 * @param partner персона
	 * @param partner2 транзитное событие
	 */
	private void refreshTabs(Event partner, Event partner2) {
		//планеты
		Control[] controls = grPlanets.getChildren();
		Table table = (Table)controls[0];
		table.removeAll();
		if (partner != null) {
			Collection<Planet> planets = partner.getPlanets().values();
			folder.setSelection(1);
			for (Planet planet : planets) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, planet.getName());
				item.setText(1, String.valueOf(planet.getLongitude()));
				//планеты партнёра
				if (partner2 != null) {
					planet = (Planet)partner2.getPlanets().get(planet.getId());
					item.setText(2, String.valueOf(planet.getLongitude()));
				}
			}
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).pack();
		} else
			folder.setSelection(0);
			
		//дома
		controls = grHouses.getChildren();
		table = (Table)controls[0];
		table.removeAll();
		if (partner != null) {
			for (House house : partner.getHouses().values()) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, house.getName());		
				item.setText(1, String.valueOf(house.getLongitude()));
				//дома партнёра
				if (partner2 != null) {
					House house2 = (House)partner2.getHouses().get(house.getId());
					item.setText(2, String.valueOf(house2.getLongitude()));
				}
			}
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).pack();
		}

		//транзиты
		table = transitViewer.getTable();
		table.removeAll();
		try {
			showBusy(true);
			transitViewer.setInput(transitData);
			table = transitViewer.getTable();
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).pack();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			showBusy(false);
		}
	}
	
	@Override
	public void initFilter(Composite parent) {
		grFilter = new Group(parent, SWT.NONE);
		grFilter.setText("Поиск");
		grFilter.setLayout(new GridLayout());
		grFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Text txSearch = new Text(grFilter, SWT.BORDER);
		new InfoDecoration(txSearch, SWT.TOP | SWT.LEFT);
		txSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txSearch.setFocus();

		EventProposalProvider proposalProvider = new EventProposalProvider(new Object[] {0}, null);
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txSearch, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Event event = (Event)((EventContentProposal)proposal).getObject();
				if (event != null)
					addModel(event);
			}
		});
	    
		Group secEvent = new Group(parent, SWT.NONE);
		secEvent.setText("Новое событие");

		lbName = new Label(secEvent, SWT.NONE);
		lbName.setText(Messages.getString("PersonView.Name")); //$NON-NLS-1$
		txName = new Text(secEvent, SWT.BORDER);

		lbBirth = new Label(secEvent, SWT.NONE);
		lbBirth.setText(Messages.getString("PersonView.BirthDate")); //$NON-NLS-1$
		dtBirth = new DateTime(secEvent, SWT.DROP_DOWN);
//		dtBirth.setNullText(""); //$NON-NLS-1$
//		dtBirth.setSelection(new Date());

		Button bt = new Button(secEvent, SWT.NONE);
		bt.setText("Расчёт");
		bt.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				trevent = null;
				onCalc(2);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		Group secPlace = new Group(secEvent, SWT.NONE);
		secPlace.setText(Messages.getString("PersonView.Place")); //$NON-NLS-1$
		txPlace = new Text(secPlace, SWT.BORDER);
		new InfoDecoration(txPlace, SWT.TOP | SWT.LEFT);

		Label lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Latitude")); //$NON-NLS-1$
		txLatitude = new Text(secPlace, SWT.BORDER);
		txLatitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Longitude")); //$NON-NLS-1$
		txLongitude = new Text(secPlace, SWT.BORDER);
		txLongitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Greenwith")); //$NON-NLS-1$
		txGreenwich = new Text(secPlace, SWT.BORDER);
		txGreenwich.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Zone")); //$NON-NLS-1$
		txZone = new Text(secPlace, SWT.BORDER);

		txDescr = new Text(secEvent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);

		bt = new Button(secEvent, SWT.NONE);
		bt.setText("Добавить");
		bt.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				syncModel(Handler.MODE_SAVE);
				addModel(trevent);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(secPlace);
		GridDataFactory.fillDefaults().span(4, 1).grab(true, false).applyTo(secPlace);
		secEvent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(secEvent);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txName);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
//			grab(true, false).applyTo(dtBirth);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			span(4, 1).grab(true, false).applyTo(txPlace);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLatitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLongitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txZone);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txGreenwich);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(false, false).applyTo(bt);
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).
			hint(SWT.DEFAULT, 48).grab(true, false).applyTo(txDescr);
	}

	@Override
	public void onCalc(Object mode) {
		MODE_CALC = (int)mode;
		if (null == trevent)
			syncModel(MODE_CALC);
		trevent.init(true);
		aged = new ArrayList<SkyPointAspect>();

		Event first = trevent;
		Event second = person;
		if (mode.equals(1)) {
			first = person;
			second = trevent;
		}
		try {
			aspects = new AspectService().getList();
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		makeTransits(first, second);
		setTransitData(aged);
		trevent.setAspectList(aged);
		refreshCard(second, first);
		refreshTabs(second, first);
	}

	/**
	 * Расчёт и отображения транзитов события
	 * @param person персона
	 * @param event событие
	 */
	public void onCalc(Event person, Event event) {
		refreshCard(person, event);
		refreshTabs(person, event);
	}

	@Override
	public void addModel(Model model) {
		try {
			super.addModel(model);
			if (null == model.getId()) {
				((Event)model).calc(false);
				model = new EventService().save(model);
			}
			//сразу сохраняем транзит в базу
			Transit transit = new Transit();
			transit.setEventid(model.getId());
			transit.setPersonid(person.getId());
			transit.setDescription(txDescr.getText());
			new TransitService().save(transit);
			reset();
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Инициализация представления местности события
	 * @param place местность
	 */
	private void initPlace(Place place) {
		if (null == place) return;
		txPlace.setText(place.getName());
		txLatitude.setText(CalcUtil.formatNumber("###.##", place.getLatitude())); //$NON-NLS-1$
		txLongitude.setText(CalcUtil.formatNumber("###.##", place.getLongitude())); //$NON-NLS-1$
		txGreenwich.setText(CalcUtil.formatNumber("###.##", place.getGreenwich())); //$NON-NLS-1$
		txZone.setText(String.valueOf(place.getGreenwich()));
	}

	/**
	 * Синхронизация события с представлением
	 * @param mode режим отображения транзитов
	 */
	private void syncModel(int mode) {
		try {
			if (!check(mode)) return;

			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.DAY_OF_MONTH, dtBirth.getDay());
			calendar.set(Calendar.MONTH, dtBirth.getMonth());
			calendar.set(Calendar.YEAR, dtBirth.getYear());

			trevent = new Event();
			trevent.setBirth(calendar.getTime());
			trevent.setPlace(trplace);
			double zone = (txZone.getText() != null && txZone.getText().length() > 0) ? Double.parseDouble(txZone.getText()) : 0;
			trevent.setZone(zone);

			if (Handler.MODE_SAVE == mode) {
				trevent.setName(txName.getText());
				trevent.setBio(txDescr.getText());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Проверка правильности атрибутов события
	 * @return true|false параметры заполнены верно|не верно
	 * @throws Exception
	 */
	@Override
	public boolean check(int mode) throws Exception {
		StringBuffer msgBody = new StringBuffer();
//		if (null == dtBirth.getSelection())
//			msgBody.append(lbBirth.getText());
		if (null == trplace) {
			DialogUtil.alertWarning(Messages.getString("EventView.PlaceIsWrong"));
			return false;
		}
		if (Handler.MODE_SAVE == mode) {
			if (txName.getText().length() == 0) 
				msgBody.append(lbName.getText());
		}
		if (msgBody.length() > 0) {
			DialogUtil.alertWarning(GUIutil.SOME_FIELDS_NOT_FILLED + msgBody);
			return false;
		} else return true;
	}

	/**
	 * Возвращает режим представления для расчёта
	 * @return 0 - планеты человека в домах события,
	 * 1 - планеты события в домах человека
	 */
	public int getModeCalc() {
		return MODE_CALC;
	}

	/**
	 * Поиск персоны, для которой рассчитывается транзит
	 * @return персона
	 */
	public Event getPerson() {
		return person;
	}

	@Override
	public void reset() {
		txName.setText(""); //$NON-NLS-1$
		txPlace.setText(""); //$NON-NLS-1$
		txLatitude.setText(""); //$NON-NLS-1$
		txLongitude.setText(""); //$NON-NLS-1$
		txZone.setText(""); //$NON-NLS-1$
		txGreenwich.setText(""); //$NON-NLS-1$
		txDescr.setText(""); //$NON-NLS-1$

		Calendar calendar = Calendar.getInstance();
		dtBirth.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * Поиск столбцов таблицы транзитов
	 * @return массив наименований столбцов
	 */
	public static String[] getTableColumns() {
		return new String[] {
			"Возраст",
			"Транзитная точка",
			"Аспект",
			"Натальная точка",
			"Направление",
			"Величина аспекта",
			"Знак Зодиака",
			"Дом",
			"Описание"
		};
	}

	@Override
	public ListSelectionListener getSelectionListener() {
		return new ListSelectionListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty()) {
					IStructuredSelection selection = (IStructuredSelection)event.getSelection();
					if (selection.getFirstElement() != null)
						trevent = (Event)selection.getFirstElement();
				}
			}
		};
	}

	/**
	 * Инициализация местностей
	 */
	private void setPlaces() {
	    PlaceProposalProvider proposalProvider = new PlaceProposalProvider();
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txPlace, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Place place = (Place)((PlaceContentProposal)proposal).getObject();
				if (place != null) {
					trplace = place;
					initPlace(place);
				}
			}
		});
	}

	@Override
	public Event getModel() {
		if (null == trevent)
			syncModel(MODE_CALC);
		if (null == trevent.getId())
			trevent.calc(false);
//		if (null == trevent.getEvent())
//			trevent.init(false);
		return trevent;
	}

	/**
	 * Инициализация транзитов планет
	 * @param data массив данных
	 */
	public void setTransitData(Object data) {
		transitData = data;
	}

	@Override
	protected void initControls() throws DataAccessException {
		super.initControls();
		setPlaces();
	}

	/**
	 * Инициализация текущего момента в качестве даты транзита
	 */
	public void initDate() {
		Calendar calendar = Calendar.getInstance();
		dtBirth.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		syncModel(MODE_CALC);
	}

	public Date getDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, dtBirth.getDay());
		calendar.set(Calendar.MONTH, dtBirth.getMonth());
		calendar.set(Calendar.YEAR, dtBirth.getYear());
		return calendar.getTime();
	}

	public void setModel(Event event) {
		trevent = event;
	}

	public void resetEvent() {
		trevent = null;
	}

	private List<SkyPointAspect> aged;

	/**
	 * Определение аспекта между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private void calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим угол между точками космограммы
			double one = point1.getLongitude();
			double two = point2.getLongitude();
			double res = CalcUtil.getDifference(one, two);
			if (point2 instanceof House) {
				if ((res >= 179 && res < 180)
						|| CalcUtil.compareAngles(one, two))
					++res;
			}
//			if (31 == point1.getId() && 158 == point2.getId())
//				System.out.println(one + "-" + two + "=" + res);

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;

				//соединения Солнца не рассматриваем
				if (a.getPlanetid() > 0)
					continue;

				if (a.isExact(res)) {
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAspect(a);
					aspect.setExact(true);
					aged.add(aspect);
				}
			}
		} catch (Exception e) {
			DialogUtil.alertWarning(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
	}

	/**
	 * Расчёт транзитов
	 */
	private void makeTransits(Event first, Event second) {
		Collection<Planet> trplanets = first.getPlanets().values();
		Collection<Planet> splanets = second.getPlanets().values();
		Collection<House> shouses = second.getHouses().values();
		for (Planet trplanet : trplanets) {
			//дирекции планеты к планетам партнёра
			for (Planet planet : splanets)
				calc(trplanet, planet);
			//дирекции планеты к куспидам домов
			for (House house : shouses)
				calc(trplanet, house);
		}
	}

	@Override
	public Model createModel() {
		return null;
	}

	/**
	 * Инициализация вкладок космограммы
	 * @return массив вкладок
	 */
	private Tab[] initTabs2() {
		Tab[] tabs = new Tab[2];
		//космограмма транзитов
		Tab tab = new Tab();
		tab.name = "Транзиты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.runner", "icons/configure.gif").createImage();
		Group group = new Group(folder2, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmpCosmogram = new CosmogramComposite(group, SWT.NONE);
		tab.control = group;
		GridLayoutFactory.swtDefaults().applyTo(group);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		tabs[0] = tab;

		//космограмма транзитного события
		tab = new Tab();
		tab.name = "Транзитный день";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/planet.gif").createImage();
		group = new Group(folder2, SWT.NONE);
		cmpCosmogram2 = new CosmogramComposite(group, SWT.NONE);
		tab.control = group;
		GridLayoutFactory.swtDefaults().applyTo(group);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		tabs[1] = tab;

		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).
			hint(514, 514).span(3, 1).grab(true, false).applyTo(cmpCosmogram);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).
			hint(514, 514).span(3, 1).grab(true, false).applyTo(cmpCosmogram2);

		return tabs;
	}
}
