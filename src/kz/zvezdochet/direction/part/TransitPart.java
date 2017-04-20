package kz.zvezdochet.direction.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
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
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.service.EventService;
import kz.zvezdochet.util.Configuration;

/**
 * Представление транзитов
 * @author Nataly Didenko
 *
 */
public class TransitPart extends ModelListView implements ICalculable {
	@Inject
	public TransitPart() {}

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
	private CDateTime dtBirth;
	private Text txDescr;
	private TableViewer transitViewer;
	
	/**
	 * Режим расчёта транзитов.
	 * 1 - режим планет события в карте персоны
	 * 2 - по умолчанию отображаются планеты персоны в карте события
	 */
	private int MODE_CALC = 2;

	private CosmogramComposite cmpCosmogram;
	private CTabFolder folder;
	private Group grPlanets;
	private Group grHouses;
	private Group grAspectType;
	private Group grTransits;

	@PostConstruct @Override
	public View create(Composite parent) {
		super.create(parent);
		Group grCosmogram = new Group(parent, SWT.NONE);
		grCosmogram.setText("Космограмма");
		cmpCosmogram = new CosmogramComposite(grCosmogram, SWT.NONE);

		folder = new CTabFolder(grCosmogram, SWT.BORDER);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.setSimple(false);
		folder.setUnselectedCloseVisible(false);
		Tab[] tabs = initTabs();
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
		return null;
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

	@Override
	protected void init(Composite parent) {
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
	}

	/**
	 * Перерисовка космограммы
	 * @param event событие|персона
	 * @param event2 событие|персона
	 */
	private void refreshCard(Event event, Event event2) {
		List<String> params = new ArrayList<String>();
		Map<String, String[]> types = AspectType.getHierarchy();
		for (Control control : grAspectType.getChildren()) {
			Button button = (Button)control;
			if (button.getSelection())
				params.addAll(Arrays.asList(types.get(button.getData("type"))));
		}
		if (params.size() < 1) return;
		cmpCosmogram.paint(event.getConfiguration(), event2.getConfiguration(), params);
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
		Tab[] tabs = new Tab[5];
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
		
		//аспекты
		tab = new Tab();
		tab.name = "Аспекты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect.gif").createImage();
		grAspectType = new Group(folder, SWT.NONE);
		grAspectType.setLayout(new GridLayout());
		List<Model> types = new ArrayList<Model>();
		try {
			types = new AspectTypeService().getList();
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		for (Model model : types) {
			AspectType type = (AspectType)model;
			if (type.getImage() != null) {
				final Button bt = new Button(grAspectType, SWT.BORDER | SWT.CHECK);
				bt.setText(type.getName());
				bt.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect/" + type.getImage()).createImage());
				bt.setSelection(true);
				bt.setData("type", type.getCode());
				bt.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (bt.getSelection())
							onCalc(MODE_CALC);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {}
				});
			}
		}
		tab.control = grAspectType;
		tabs[3] = tab;

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
		tabs[4] = tab;

		return tabs;
	}

	/**
	 * Обновление вкладок
	 */
	private void refreshTabs(Event partner, Event partner2) {
		//планеты
		Control[] controls = grPlanets.getChildren();
		Table table = (Table)controls[0];
		table.removeAll();
		Configuration conf = partner.getConfiguration();
		Configuration conf2 = partner2.getConfiguration();
		if (conf != null) {
			folder.setSelection(1);
			int j = -1;
			for (Model base : conf.getPlanets()) {
				++j;
				Planet planet = (Planet)base;
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, planet.getName());
				item.setText(1, String.valueOf(planet.getCoord()));
				//планеты партнёра
				if (conf2 != null) {
					planet = (Planet)conf2.getPlanets().get(j);
					item.setText(2, String.valueOf(planet.getCoord()));
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
		if (conf != null) {
			int j = -1;
			for (Model base : conf.getHouses()) {
				++j;
				House house = (House)base;
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, house.getName());		
				item.setText(1, String.valueOf(house.getCoord()));
				//дома партнёра
				if (conf2 != null) {
					house = (House)conf2.getHouses().get(j);
					item.setText(2, String.valueOf(house.getCoord()));
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
	public void initFilter() {
		grFilter = new Group(container, SWT.NONE);
		grFilter.setText("Поиск");
		grFilter.setLayout(new GridLayout());
		grFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Text txSearch = new Text(grFilter, SWT.BORDER);
		new InfoDecoration(txSearch, SWT.TOP | SWT.LEFT);
		txSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txSearch.setFocus();

		EventProposalProvider proposalProvider = new EventProposalProvider(new Object[] {0});
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
	    
		Group secEvent = new Group(container, SWT.NONE);
		secEvent.setText("Новое событие");

		lbName = new Label(secEvent, SWT.NONE);
		lbName.setText(Messages.getString("PersonView.Name")); //$NON-NLS-1$
		txName = new Text(secEvent, SWT.BORDER);

		lbBirth = new Label(secEvent, SWT.NONE);
		lbBirth.setText(Messages.getString("PersonView.BirthDate")); //$NON-NLS-1$
		dtBirth = new CDateTime(secEvent, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.TIME_MEDIUM);
		dtBirth.setNullText(""); //$NON-NLS-1$
		dtBirth.setSelection(new Date());

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
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(dtBirth);
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
//		System.out.println("onCalc" + MODE_CALC);
		if (null == trevent)
			syncModel(MODE_CALC);
		trevent.init();
		aged = new ArrayList<SkyPointAspect>();

		Event first = trevent;
		Event second = person;
		if (mode.equals(1)) {
			first = person;
			second = trevent;
		}
		makeTransits(first, second);
		setTransitData(aged);
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
				((Event)model).calc(true);
				model = new EventService().save(model);
			}
			//сразу сохраняем событие в базу
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
			trevent = new Event();
			trevent.setBirth(dtBirth.getSelection());
			trevent.setPlace(trplace);
			double zone = (txZone.getText() != null && txZone.getText().length() > 0) ? Double.parseDouble(txZone.getText()) : 0;
			trevent.setZone(zone);

			if (Handler.MODE_SAVE == mode) {
				trevent.setName(txName.getText());
				trevent.setText(txDescr.getText());
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
		if (null == dtBirth.getSelection())
			msgBody.append(lbBirth.getText());
		if (null == trplace) {
			DialogUtil.alertError(Messages.getString("EventView.PlaceIsWrong"));
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
		dtBirth.setSelection(new Date());
		txDescr.setText(""); //$NON-NLS-1$
	}

	/**
	 * Поиск столбцов таблицы транзитов
	 * @return массив наименований столбцов
	 */
	public static String[] getTableColumns() {
		return new String[] {
			"Возраст",
			"Точка 1",
			"Аспект",
			"Точка 2",
			"Направление",
			"Величина аспекта",
			"Знак Зодиака",
			"Дом"
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
		if (null == trevent.getConfiguration())
			trevent.init();
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
	protected void initControls() {
		setPlaces();
	}

	/**
	 * Инициализация текущего момента в качестве даты транзита
	 */
	public void initDate() {
		dtBirth.setSelection(new Date());
		syncModel(MODE_CALC);
	}

	public Date getDate() {
		return dtBirth.getSelection();
	}

	public void setModel(Event event) {
		trevent = event;
	}

	public void resetEvent() {
		trevent = null;
	}

	private List<SkyPointAspect> aged;

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private void calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим угол между точками космограммы
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
	
			//определяем, является ли аспект стандартным
			List<Model> aspects = new AspectService().getList();
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExactTruncAspect(res)) {
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAspect(a);
					aged.add(aspect);
				}
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
	}

	/**
	 * Расчёт транзитов
	 */
	private void makeTransits(Event first, Event second) {
		List<Model> trplanets = first.getConfiguration().getPlanets();
		for (Model model : trplanets) {
			Planet trplanet = (Planet)model;
			//дирекции планеты к планетам партнёра
			for (Model model2 : second.getConfiguration().getPlanets()) {
				Planet planet = (Planet)model2;
				calc(trplanet, planet);
			}
			//дирекции планеты к куспидам домов
			for (Model model2 : second.getConfiguration().getHouses()) {
				House house = (House)model2;
				calc(trplanet, house);
			}
		}
	}

	@Override
	public Model createModel() {
		return null;
	}
}
