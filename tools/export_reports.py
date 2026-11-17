"""Prepare printable submission PDFs. This is a document tool, not game runtime code."""
from __future__ import annotations

import html
import math
import re
from pathlib import Path

from PIL import Image as PILImage, ImageDraw
from reportlab import rl_config
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, Flowable, Frame, Image, KeepTogether, PageBreak,
    PageTemplate, Paragraph, Preformatted, Spacer, Table, TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "pdf"
RENDER = ROOT / "tmp" / "report-render"
OUT.mkdir(parents=True, exist_ok=True)
RENDER.mkdir(parents=True, exist_ok=True)
PAGE_W, PAGE_H = A4
MARGIN = 51
WIDTH = PAGE_W - 2 * MARGIN
INK = colors.HexColor("#152B3B")
TEAL = colors.HexColor("#147E7F")
AMBER = colors.HexColor("#BD8041")
MUTED = colors.HexColor("#526977")
PALE = colors.HexColor("#F1F6F5")
LINE = colors.HexColor("#D5E1E1")
WHITE = colors.white

for family, regular, bold, italic in [
    ("Body", "calibri.ttf", "calibrib.ttf", "calibrii.ttf"),
    ("Code", "consola.ttf", "consolab.ttf", "consolai.ttf"),
]:
    for suffix, filename in [("", regular), ("-Bold", bold), ("-Italic", italic)]:
        pdfmetrics.registerFont(TTFont(family + suffix, str(Path("C:/Windows/Fonts") / filename)))
    pdfmetrics.registerFontFamily(family, normal=family, bold=family + "-Bold", italic=family + "-Italic")

STYLES = {
    "body": ParagraphStyle("body", fontName="Body", fontSize=11, leading=15.5,
                           textColor=INK, spaceAfter=9, allowWidows=0, allowOrphans=0),
    "h1": ParagraphStyle("h1", fontName="Body-Bold", fontSize=20, leading=24,
                         textColor=TEAL, spaceBefore=6, spaceAfter=14, keepWithNext=True),
    "h2": ParagraphStyle("h2", fontName="Body-Bold", fontSize=13, leading=17,
                         textColor=INK, spaceBefore=13, spaceAfter=8, keepWithNext=True),
    "small": ParagraphStyle("small", fontName="Body", fontSize=9, leading=12.5,
                            textColor=MUTED, spaceAfter=7),
    "code": ParagraphStyle("code", fontName="Code", fontSize=9.2, leading=12.2,
                           textColor=INK, leftIndent=10, rightIndent=8,
                           spaceBefore=7, spaceAfter=12, borderPadding=10,
                           backColor=PALE, borderColor=LINE, borderWidth=0.5),
    "cell": ParagraphStyle("cell", fontName="Body", fontSize=9.6, leading=13.2,
                           textColor=INK, spaceAfter=0),
    "cellhead": ParagraphStyle("cellhead", fontName="Body-Bold", fontSize=9.6, leading=13,
                               textColor=WHITE, spaceAfter=0),
    "bullet": ParagraphStyle("bullet", fontName="Body", fontSize=11, leading=15.5,
                             textColor=INK, leftIndent=15, firstLineIndent=-12, spaceAfter=8),
    "toc": ParagraphStyle("toc", fontName="Body", fontSize=11, leading=21,
                          textColor=INK, leftIndent=0, rightIndent=10),
}


def inline(text: str) -> str:
    text = text.replace("\u2011", "-").replace("\u2013", "-").replace("\u2014", " - ")
    text = text.replace("\u2192", " -> ").replace("\u2194", " <-> ")
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r"\1", text)
    text = html.escape(text, quote=False)
    text = re.sub(r"`([^`]+)`", r'<font name="Code" size="9.7">\1</font>', text)
    text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
    return text


def para(text: str, style: str = "body") -> Paragraph:
    return Paragraph(inline(text), STYLES[style])


def code_wrap(source: str, limit: int = 83) -> str:
    """Wrap only at whitespace outside string literals, preserving all code tokens."""
    lines = []
    for line in source.splitlines():
        while len(line) > limit:
            quoted = None
            escaped = False
            candidates = []
            for index, char in enumerate(line):
                if escaped:
                    escaped = False
                    continue
                if char == "\\" and quoted:
                    escaped = True
                elif quoted:
                    if char == quoted:
                        quoted = None
                elif char in "\"'":
                    quoted = char
                elif char.isspace() and index > 18 and index <= limit:
                    candidates.append(index)
            if not candidates:
                break
            cut = candidates[-1]
            lines.append(line[:cut].rstrip())
            indent = len(line) - len(line.lstrip())
            line = " " * min(indent + 4, 16) + line[cut:].lstrip()
        lines.append(line)
    return "\n".join(lines)


class Rule(Flowable):
    def __init__(self):
        Flowable.__init__(self)
        self.width, self.height = WIDTH, 13

    def draw(self):
        self.canv.setStrokeColor(LINE)
        self.canv.line(0, 6, self.width, 6)


class Cover(Flowable):
    def __init__(self, kind: str):
        Flowable.__init__(self)
        self.kind = kind
        self.width, self.height = WIDTH, PAGE_H - 118

    def draw(self):
        c = self.canv
        h = self.height
        c.setFillColor(TEAL)
        c.setFont("Body-Bold", 10)
        c.drawString(0, h - 18, "JAVA 21  /  OBJECT ORIENTED PROGRAMMING")
        c.setFillColor(INK)
        c.setFont("Body-Bold", 34)
        c.drawString(0, h - 66, "AETHERIA")
        c.setFont("Body-Bold", 28)
        c.drawString(0, h - 103, "STARFALL SURVIVAL")
        c.setFillColor(TEAL)
        c.setFont("Body", 18)
        c.drawString(0, h - 136, self.kind)
        c.setStrokeColor(AMBER)
        c.setLineWidth(2)
        c.line(0, h - 155, 60, h - 155)
        c.drawImage(ImageReader(str(ROOT / "src/art/starfall-title.png")),
                    0, h - 492, width=WIDTH, height=WIDTH * 2 / 3, mask="auto")
        c.setFillColor(MUTED)
        c.setFont("Body", 8.5)
        c.drawString(0, h - 505, "Original AI-generated title illustration. Runtime sprites are generated in Java2D.")
        y = h - 539
        c.setFillColor(INK)
        c.setFont("Body-Bold", 11)
        c.drawString(0, y, "Gather. Craft. Bank diamonds. Survive.")
        c.setFillColor(MUTED)
        c.setFont("Body", 10)
        c.drawString(0, y - 22, "Three champions / three campaign levels / standard Java libraries")
        c.setStrokeColor(LINE)
        c.setLineWidth(0.5)
        c.line(0, y - 42, WIDTH, y - 42)
        c.setFillColor(INK)
        c.setFont("Body-Bold", 10)
        c.drawString(0, y - 63, "STUDENT NAMES AND REGISTRATION NUMBERS")
        c.setFont("Body", 10)
        c.drawString(0, y - 83, "Complete before submission: __________________________________")
        c.setFillColor(MUTED)
        c.setFont("Body", 9)
        c.drawString(0, y - 109, "Proposal acknowledgment, group arrangement and oral examination remain")
        c.drawString(0, y - 123, "course-process steps to be confirmed by the students.")


def node(c, x, y, w, label, detail="", height=35, interface=False):
    c.setStrokeColor(TEAL if interface else LINE)
    c.setFillColor(PALE if not interface else colors.HexColor("#E5F3F0"))
    c.roundRect(x, y, w, height, 5, stroke=1, fill=1)
    c.setFillColor(INK)
    c.setFont("Body-Bold", 9)
    c.drawCentredString(x + w / 2, y + (19 if detail else 13), label)
    if detail:
        c.setFillColor(MUTED)
        c.setFont("Body", 7.8)
        c.drawCentredString(x + w / 2, y + 7, detail)


def arrow(c, x1, y1, x2, y2, dashed=False, inheritance=False):
    c.setStrokeColor(TEAL if not dashed else AMBER)
    c.setLineWidth(0.8)
    c.setDash(3, 3) if dashed else c.setDash()
    c.line(x1, y1, x2, y2)
    c.setDash()
    angle = math.atan2(y2 - y1, x2 - x1)
    size = 5 if not inheritance else 7
    p = c.beginPath()
    p.moveTo(x2, y2)
    p.lineTo(x2 - size * math.cos(angle - 0.45), y2 - size * math.sin(angle - 0.45))
    p.lineTo(x2 - size * math.cos(angle + 0.45), y2 - size * math.sin(angle + 0.45))
    p.close()
    c.setFillColor(WHITE if inheritance else TEAL if not dashed else AMBER)
    c.drawPath(p, stroke=1, fill=1)


class ClassDiagram(Flowable):
    def __init__(self):
        Flowable.__init__(self)
        self.width, self.height = WIDTH, 590

    def draw(self):
        c = self.canv
        c.setFillColor(TEAL)
        c.setFont("Body-Bold", 12)
        c.drawString(0, 574, "Actor types and campaign ownership")
        c.setFont("Body", 9)
        c.setFillColor(MUTED)
        c.drawString(0, 557, "Open triangles point to parents. Filled diamonds mark ownership.")
        for x in [66, 242, 416]:
            arrow(c, x, 456, 242, 500, inheritance=True)
        for x in [273, 360, 449]:
            arrow(c, x, 375, 416, 421, inheritance=True)
        arrow(c, 402, 310, 449, 340, inheritance=True)
        arrow(c, 466, 310, 449, 340, inheritance=True)
        arrow(c, 66, 421, 66, 375)
        node(c, 188, 500, 108, "Entity", "abstract spatial actor")
        node(c, 0, 421, 132, "Champion", "one configured explorer")
        node(c, 187, 421, 110, "Projectile", "gun pulse")
        node(c, 350, 421, 132, "Enemy", "abstract hostile")
        node(c, 0, 340, 132, "ChampionType", "Astra / Nyx / Orion")
        node(c, 238, 340, 70, "Alien", "monster")
        node(c, 325, 340, 70, "Bat", "flying monster")
        node(c, 405, 340, 88, "MeleeEnemy", "abstract attack cycle")
        node(c, 374, 275, 56, "Orc", "melee enemy")
        node(c, 438, 275, 55, "Skeleton", "melee enemy")
        c.setFillColor(MUTED)
        c.setFont("Body", 9)
        c.drawString(0, 310, "Champion and Enemy implement Damageable.")
        c.drawString(0, 292, "Each Enemy references its Champion target.")
        c.drawString(0, 274, "EncounterFactory selects and configures opponents.")
        c.setStrokeColor(LINE)
        c.line(0, 253, WIDTH, 253)
        node(c, 164, 205, 150, "GameSession", "owns world / actors / campaign")
        for x, width, label, detail in [
            (0, 145, "Chunk", "terrain + hidden resources"),
            (164, 145, "CampaignProgress", "achievements + secured cargo"),
            (328, 158, "ShipDock", "safe interaction destination"),
        ]:
            arrow(c, 239, 205, x + width / 2, 167)
            node(c, x, 132, width, label, detail)
        c.setFillColor(TEAL)
        p = c.beginPath()
        p.moveTo(239, 205)
        p.lineTo(234, 200)
        p.lineTo(239, 195)
        p.lineTo(244, 200)
        p.close()
        c.drawPath(p, stroke=0, fill=1)
        arrow(c, 220, 132, 210, 97)
        arrow(c, 407, 132, 440, 97, dashed=True, inheritance=True)
        node(c, 164, 62, 96, "Inventory", "materials + equipment")
        node(c, 392, 62, 94, "Interactable", "interface", interface=True)
        p = c.beginPath()
        p.moveTo(220, 132)
        p.lineTo(215, 127)
        p.lineTo(217, 121)
        p.lineTo(223, 126)
        p.close()
        c.drawPath(p, stroke=0, fill=1)
        c.setFillColor(MUTED)
        c.setFont("Body", 8.2)
        c.drawString(0, 38, "ShipDock references the same campaign; this is an association.")
        c.drawString(0, 22, "The session also owns the champion and enemy/projectile collections.")


class SequenceDiagram(Flowable):
    def __init__(self):
        Flowable.__init__(self)
        self.width, self.height = WIDTH, 340

    def draw(self):
        c = self.canv
        labels = ["Panel", "Session", "Explorer", "Chunk", "Enemy", "Campaign", "ShipDock"]
        xs = [22, 94, 167, 239, 312, 385, 463]
        c.setFillColor(TEAL)
        c.setFont("Body-Bold", 12)
        c.drawString(0, 322, "Example combat, crafting and banking sequence")
        for x, label in zip(xs, labels):
            node(c, x - 29, 277, 58, label, height=26)
            c.setStrokeColor(LINE)
            c.setDash(2, 3)
            c.line(x, 277, x, 11)
            c.setDash()
        messages = [
            (0, 1, "update()"),
            (1, 2, "update(world)"),
            (2, 3, "canOccupy(bounds)"),
            (1, 3, "collectAt / harvestAt after F"),
            (1, 5, "gather(resource, amount)"),
            (1, 2, "strike(enemy)"),
            (2, 4, "takeDamage via Damageable"),
            (1, 5, "recordKill(monster)"),
            (0, 1, "craft() after checklist"),
            (1, 5, "craft() and equip owned weapon"),
            (0, 1, "interact() at ship"),
            (1, 6, "Interactable.interact(explorer)"),
            (6, 5, "depositDiamonds()"),
            (1, 1, "offer stay / advance"),
        ]
        for row, (source, target, text) in enumerate(messages):
            y = 251 - row * 18
            x1, x2 = xs[source], xs[target]
            if source == target:
                c.setStrokeColor(TEAL)
                c.line(x1, y, x1 + 18, y)
                c.line(x1 + 18, y, x1 + 18, y - 8)
                arrow(c, x1 + 18, y - 8, x1, y - 8)
            else:
                arrow(c, x1, y, x2, y)
            c.setFillColor(INK)
            c.setFont("Body", 8.1)
            label_width = pdfmetrics.stringWidth(text, "Body", 8.1)
            left = x1 + 23 if source == target else max(0, min((x1 + x2 - label_width) / 2, WIDTH - label_width))
            c.setFillColor(WHITE)
            c.rect(left - 2, y + 2, label_width + 4, 11, stroke=0, fill=1)
            c.setFillColor(INK)
            c.drawString(left, y + 4, text)


class ReportDoc(BaseDocTemplate):
    def __init__(self, path, label):
        super().__init__(str(path), pagesize=A4, leftMargin=MARGIN, rightMargin=MARGIN,
                         topMargin=52, bottomMargin=48, title=f"Aetheria: Starfall Survival - {label}",
                         author="Aetheria project team", subject="Java 21 OOP project submission")
        self.label = label
        frame = Frame(MARGIN, 48, WIDTH, PAGE_H - 100, leftPadding=0, bottomPadding=0,
                      rightPadding=0, topPadding=0)
        self.addPageTemplates(PageTemplate(id="pages", frames=frame, onPage=self.chrome))

    def chrome(self, c, _doc):
        if self.page == 1:
            return
        c.saveState()
        c.setFillColor(TEAL)
        c.setFont("Body-Bold", 8)
        c.drawString(MARGIN, PAGE_H - 31, "AETHERIA / STARFALL SURVIVAL")
        c.setFillColor(MUTED)
        c.setFont("Body", 8)
        c.drawRightString(PAGE_W - MARGIN, PAGE_H - 31, self.label.upper())
        c.setStrokeColor(LINE)
        c.setLineWidth(0.4)
        c.line(MARGIN, PAGE_H - 39, PAGE_W - MARGIN, PAGE_H - 39)
        c.line(MARGIN, 35, PAGE_W - MARGIN, 35)
        c.setFillColor(MUTED)
        c.drawString(MARGIN, 23, "Java 21 / self-contained desktop game")
        c.drawRightString(PAGE_W - MARGIN, 23, str(self.page))
        c.restoreState()

    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and flowable.style.name == "h1":
            text = flowable.getPlainText()
            if text == "Index":
                return
            key = re.sub(r"[^a-z0-9]+", "-", text.lower())
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(text, key, 0)
            self.notify("TOCEntry", (0, text, self.page, key))


def make_table(lines):
    values = [[part.strip() for part in line.strip().strip("|").split("|")] for line in lines]
    values = [row for row in values if not all(re.fullmatch(r"[: -]+", cell) for cell in row)]
    count = len(values[0])
    if count == 2:
        widths = [WIDTH * 0.40, WIDTH * 0.60]
    elif count == 3:
        widths = [WIDTH * 0.16, WIDTH * 0.42, WIDTH * 0.42]
    elif count == 6:
        widths = [WIDTH * value for value in (0.08, 0.12, 0.14, 0.23, 0.26, 0.17)]
    elif count == 5:
        widths = [WIDTH * value for value in (0.08, 0.19, 0.25, 0.30, 0.18)]
    else:
        widths = [WIDTH / count] * count
    data = [[para(cell, "cellhead" if row_index == 0 else "cell")
             for cell in row] for row_index, row in enumerate(values)]
    table = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    padding = 8 if count >= 5 else 9
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), TEAL),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), padding),
        ("RIGHTPADDING", (0, 0), (-1, -1), padding),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, PALE]),
        ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#D9D9D9")),
        ("LINEBELOW", (0, 0), (-1, 0), 0.7, TEAL),
        ("LINEBELOW", (0, 1), (-1, -1), 0.25, LINE),
    ]))
    return table


def blocks(text):
    lines = text.splitlines()
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line:
            index += 1
            continue
        if line.startswith("```"):
            language = line[3:]
            content = []
            index += 1
            while index < len(lines) and not lines[index].strip().startswith("```"):
                content.append(lines[index])
                index += 1
            yield "code", (language, "\n".join(content))
            index += 1
            continue
        if line.startswith("|"):
            table_lines = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_lines.append(lines[index])
                index += 1
            yield "table", table_lines
            continue
        if line.startswith("#"):
            level = len(line) - len(line.lstrip("#"))
            yield "heading", (level, line[level:].strip())
            index += 1
            continue
        if re.match(r"^(?:- |\d+\. )", line):
            yield "list", line
            index += 1
            continue
        if line.startswith("> "):
            yield "quote", line[2:]
            index += 1
            continue
        paragraph = [line]
        index += 1
        while index < len(lines) and lines[index].strip() and not re.match(r"^(?:#|\||```|- |\d+\. )", lines[index].strip()):
            paragraph.append(lines[index].strip())
            index += 1
        yield "paragraph", " ".join(paragraph)


def build(kind):
    report = kind == "report"
    oral = kind == "oral"
    source_name, filename, label = {
        "report": ("PROJECT_REPORT.md", "Aetheria-Starfall-Project-Report.pdf", "Project report"),
        "guide": ("USER_GUIDE.md", "Aetheria-Starfall-User-Guide.pdf", "User guide"),
        "oral": ("ORAL_EXAM_GUIDE.md", "Aetheria-Starfall-Oral-Exam-Guide.pdf", "Oral exam study guide"),
    }[kind]
    source = ROOT / "docs" / source_name
    story = [Cover(label), PageBreak()]
    if report:
        story.append(para("Index", "h1"))
        toc = TableOfContents()
        toc.levelStyles = [STYLES["toc"]]
        story.extend([toc, Spacer(1, 18), Rule(),
                      para("Course basis: the supplied OOP introduction, slides 8-12. Sections 3-13 separately demonstrate the eleven required concepts through source examples.", "small"),
                      PageBreak()])
    active = False
    current_section = 0
    report_breaks = {1, 2, 3, 5, 7, 9, 11, 13, 15, 18}
    guide_breaks = {"Three levels", "Controls", "Combat and survival", "Save and load"}
    section_count = 0
    for block_type, value in blocks(source.read_text(encoding="utf-8")):
        if block_type == "heading":
            level, heading = value
            if report:
                match = re.match(r"(\d+)\.", heading)
                if level == 2 and match:
                    active = True
                    current_section = int(match.group(1))
                    if section_count and current_section in report_breaks:
                        story.append(PageBreak())
                    story.append(para(heading, "h1"))
                    section_count += 1
                elif active:
                    story.append(para(heading, "h2"))
            else:
                if level == 2:
                    active = True
                    oral_break = oral and heading.startswith(("4.", "6."))
                    if section_count and (oral_break or not oral and heading in guide_breaks):
                        story.append(PageBreak())
                    story.append(para(heading, "h1"))
                    section_count += 1
                elif active and level > 2:
                    story.append(para(heading, "h2"))
            continue
        if not active:
            continue
        if block_type == "code":
            language, content = value
            if language == "mermaid":
                if content.startswith("classDiagram"):
                    story.append(ClassDiagram())
                    story.append(para("Figure 1. Actor inheritance, configuration, combat capability and campaign ownership. Source: the submitted Java classes.", "small"))
                elif content.startswith("sequenceDiagram"):
                    story.append(SequenceDiagram())
                    story.append(para("Figure 2. Combat, gathering, crafting and safe banking across several actions. The sequence follows the report's source diagram rather than one simulation tick.", "small"))
            else:
                wrapped = code_wrap(content)
                story.append(Preformatted(wrapped, STYLES["code"]))
        elif block_type == "table":
            story.extend([make_table(value), Spacer(1, 12)])
        elif block_type == "list":
            text = value
            if text.startswith("- "):
                text = "-  " + text[2:]
            story.append(para(text, "bullet"))
        elif block_type == "quote":
            quote = Table([[para(value)]], colWidths=[WIDTH - 12], hAlign="LEFT")
            quote.setStyle(TableStyle([
                ("LINEBEFORE", (0, 0), (0, 0), 2, TEAL),
                ("LEFTPADDING", (0, 0), (-1, -1), 12),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ]))
            story.extend([quote, Spacer(1, 9)])
        else:
            if value.startswith("For viewers without Mermaid"):
                continue
            story.append(para(value))
    doc = ReportDoc(OUT / filename, label)
    doc.multiBuild(story)
    return OUT / filename


def render(paths):
    import pypdfium2 as pdfium
    from pypdf import PdfReader
    metrics = []
    for path in paths:
        reader = PdfReader(path)
        pdf = pdfium.PdfDocument(str(path))
        pages = []
        for number, page in enumerate(pdf):
            pil = page.render(scale=1.35).to_pil().convert("RGB")
            target = RENDER / f"{path.stem}-{number + 1:02d}.png"
            pil.save(target)
            thumb = pil.copy()
            thumb.thumbnail((298, 422))
            pages.append(thumb)
        columns = 4 if len(pages) > 8 else 3
        rows = math.ceil(len(pages) / columns)
        sheet = PILImage.new("RGB", (columns * 322, rows * 456), "#dce5e7")
        draw = ImageDraw.Draw(sheet)
        for index, thumb in enumerate(pages):
            x = (index % columns) * 322 + 12
            y = (index // columns) * 456 + 22
            sheet.paste(thumb, (x, y))
            draw.text((x, y - 16), f"Page {index + 1}", fill="#153342")
        contact = RENDER / f"{path.stem}-contact.png"
        sheet.save(contact)
        text = "\n".join(page.extract_text() or "" for page in reader.pages)
        (RENDER / f"{path.stem}.txt").write_text(text, encoding="utf-8")
        metrics.append({"file": str(path), "pages": len(reader.pages), "bytes": path.stat().st_size,
                        "contact": str(contact), "characters": len(text)})
    return metrics


if __name__ == "__main__":
    import json
    outputs = [build("report"), build("guide"), build("oral")]
    print(json.dumps(render(outputs), indent=2))
