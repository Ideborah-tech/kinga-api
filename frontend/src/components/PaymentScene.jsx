import { useEffect, useRef } from 'react';

// Looping 3D scene for the sign-in page, readable at a glance:
// a phone ("Your app") sends a payment to the provider's servers.
//   1. Payments go through.
//   2. The provider goes down and the payment fails.
//   3. The KingaAPI shield checks the provider: "Not your code".
const LOOP = 12000;
const FAIL_AT = 4500;
const VERDICT_AT = 8500;
const TRIP = 2250;
const CAMERA = 9;

const C = {
  phone: { front: '#6c3ce9', top: '#8b66ff', side: '#4b23b8', sideDark: '#3a1c94' },
  server: { front: '#ffffff', top: '#f1ecff', side: '#cbb8ff', sideDark: '#9b7bff' },
  shieldFront: '#ffffff',
  shieldBack: '#3a1c94',
  sideDark: [58, 28, 148],
  sideLight: [139, 102, 255],
  accent: '#6c3ce9',
  sun: '#ffb224',
  floor: '#1b0d47',
  path: 'rgba(203, 184, 255, 0.5)',
  ok: '#13a35a',
  okBright: '#3ddc84',
  bad: '#e5484d',
  badBright: '#ff6b6f',
  ink: '#1e1633',
  muted: '#6a6283',
  chip: '#33197a',
  chipText: '#e6dcff',
  white: '#ffffff',
};

// The logo's shield and "K", in its own 32x32 coordinates.
function bezier(p0, p1, p2, p3, n) {
  const out = [];
  for (let i = 1; i <= n; i++) {
    const t = i / n;
    const u = 1 - t;
    out.push([
      u * u * u * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t * t * t * p3[0],
      u * u * u * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t * t * t * p3[1],
    ]);
  }
  return out;
}
const OUTLINE = [
  [16, 4.8], [25, 8.2], [25, 11.5], [25, 14.8],
  ...bezier([25, 14.8], [25, 20.4], [21.2, 25], [16, 27.2], 10),
  ...bezier([16, 27.2], [10.8, 25], [7, 20.4], [7, 14.8], 10),
  [7, 11.5], [7, 8.2], [11.5, 6.5],
];
const K_LINES = [
  [[12.4, 10.6], [12.4, 21.4]],
  [[19.4, 10.8], [14.6, 16], [19.4, 21.2]],
];
const SHIELD_UNIT = 0.045;
const toShield = ([x, y]) => [(x - 16) * SHIELD_UNIT, -(y - 16) * SHIELD_UNIT];

const rotY = (p, a) => {
  const c = Math.cos(a), s = Math.sin(a);
  return [p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c];
};
const rotX = (p, a) => {
  const c = Math.cos(a), s = Math.sin(a);
  return [p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c];
};
const sub = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

function pill(ctx, x, y, text, bg, fg) {
  ctx.font = '600 12px "IBM Plex Sans", system-ui, sans-serif';
  const w = ctx.measureText(text).width + 20;
  roundRect(ctx, x - w / 2, y - 12, w, 24, 12);
  ctx.fillStyle = bg;
  ctx.fill();
  ctx.fillStyle = fg;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(text, x, y + 0.5);
  ctx.textAlign = 'left';
}

export default function PaymentScene({ className }) {
  const ref = useRef(null);

  useEffect(() => {
    const canvas = ref.current;
    const ctx = canvas.getContext('2d');
    const outline = OUTLINE.map(toShield);
    const kLines = K_LINES.map((l) => l.map(toShield));
    const kDot = toShield([21.4, 16]);
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const STILL = VERDICT_AT + 1500;

    let w = 0;
    let h = 0;
    let dpr = 1;
    let frame = 0;
    const resize = () => {
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      w = canvas.clientWidth;
      h = canvas.clientHeight;
      canvas.width = Math.round(w * dpr);
      canvas.height = Math.round(h * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      if (reduced) draw(STILL);
    };

    function draw(t) {
      const phase = t % LOOP;
      const down = phase >= FAIL_AT;
      const verdict = phase >= VERDICT_AT;
      const trip = (phase % TRIP) / TRIP;
      const returning = trip >= 0.5;

      const compact = h < 320;
      const S = compact ? h * 0.2 : Math.min(w * 0.13, h * 0.17);
      const cx = compact ? w * 0.6 : w * 0.46;
      const cy = compact ? h * 0.56 : h * 0.47;

      // Camera: the whole scene sways gently so it reads as 3D
      const yawG = 0.22 * Math.sin((2 * Math.PI * t) / 14000);
      const pitchG = 0.2;
      const world = (p) => rotX(rotY(p, yawG), pitchG);
      const project = (p) => {
        const k = CAMERA / (CAMERA - p[2]);
        return { x: cx + p[0] * S * k, y: cy - p[1] * S * k, k };
      };
      const at = (p) => project(world(p));

      const poly = (pts, fill) => {
        ctx.beginPath();
        pts.forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)));
        ctx.closePath();
        ctx.fillStyle = fill;
        ctx.fill();
      };

      // A box with flat-shaded faces; returns its projected front face for decoration.
      const box = (pos, size, yaw, colors) => {
        const [a, b, c] = size.map((v) => v / 2);
        const local = [[-a, b, c], [a, b, c], [a, -b, c], [-a, -b, c], [-a, b, -c], [a, b, -c], [a, -b, -c], [-a, -b, -c]];
        const cam = local.map((p) => {
          const r = rotY(p, yaw);
          return world([r[0] + pos[0], r[1] + pos[1], r[2] + pos[2]]);
        });
        const faces = [
          { i: [5, 4, 7, 6], c: colors.side },
          { i: [3, 2, 6, 7], c: colors.side },
          { i: [4, 0, 3, 7], c: colors.side },
          { i: [1, 5, 6, 2], c: colors.sideDark },
          { i: [4, 5, 1, 0], c: colors.top },
          { i: [0, 1, 2, 3], c: colors.front },
        ];
        const eye = [0, 0, CAMERA];
        faces.forEach((f) => {
          const [p0, p1, p2] = f.i.map((k) => cam[k]);
          const outward = cross(sub(p1, p0), sub(p2, p0)).map((v) => -v);
          if (dot(outward, sub(eye, p0)) > 0) poly(f.i.map((k) => project(cam[k])), f.c);
        });
        return [0, 1, 3].map((k) => project(cam[k]));
      };

      // Draw on a projected face as if it were a flat W x H canvas.
      const onFace = ([tl, tr, bl], W, H, paint) => {
        ctx.save();
        ctx.setTransform(
          (dpr * (tr.x - tl.x)) / W, (dpr * (tr.y - tl.y)) / W,
          (dpr * (bl.x - tl.x)) / H, (dpr * (bl.y - tl.y)) / H,
          dpr * tl.x, dpr * tl.y,
        );
        paint();
        ctx.restore();
      };

      const shadow = (x, z, rx) => {
        const p = at([x, -1.2, z]);
        ctx.fillStyle = C.floor;
        ctx.beginPath();
        ctx.ellipse(p.x, p.y, rx * S * p.k, 0.16 * S * p.k, 0, 0, Math.PI * 2);
        ctx.fill();
      };

      ctx.clearRect(0, 0, w, h);

      const bob = 0.07 * Math.sin((2 * Math.PI * t) / 3000);
      const PHONE = [-2, 0.05 + bob, 0];
      const SERVER_X = 2;
      shadow(PHONE[0], 0, 0.6);
      shadow(SERVER_X, 0, 0.95);

      // The phone and its payment screen
      const phoneFace = box(PHONE, [1, 2, 0.14], 0.38, C.phone);
      const status = !returning ? 'sending' : down ? 'failed' : 'paid';
      const screenStatus = verdict ? 'failed' : status;
      onFace(phoneFace, 200, 400, () => {
        ctx.fillStyle = C.white;
        roundRect(ctx, 12, 14, 176, 372, 22);
        ctx.fill();
        ctx.fillStyle = C.ink;
        roundRect(ctx, 80, 24, 40, 8, 4);
        ctx.fill();
        ctx.textBaseline = 'alphabetic';
        ctx.fillStyle = C.muted;
        ctx.font = '500 16px "IBM Plex Sans", system-ui, sans-serif';
        ctx.fillText('Send money', 28, 78);
        ctx.fillStyle = C.ink;
        ctx.font = '700 32px "IBM Plex Sans", system-ui, sans-serif';
        ctx.fillText('KES 1,500', 28, 118);
        ctx.fillStyle = C.muted;
        ctx.font = '400 14px "IBM Plex Sans", system-ui, sans-serif';
        ctx.fillText('To: Amani Shop', 28, 144);

        const look = {
          sending: ['#f1ecff', C.accent, 'Sending…'],
          paid: ['#e3f7ec', C.ok, '✓  Payment sent'],
          failed: ['#fdecec', C.bad, '✕  Payment failed'],
        }[screenStatus];
        ctx.fillStyle = look[0];
        roundRect(ctx, 28, 196, 144, 56, 14);
        ctx.fill();
        ctx.fillStyle = look[1];
        ctx.font = '600 16px "IBM Plex Sans", system-ui, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(look[2], 100, 225);

        ctx.fillStyle = C.accent;
        roundRect(ctx, 28, 318, 144, 46, 14);
        ctx.fill();
        ctx.fillStyle = C.white;
        ctx.fillText('Pay', 100, 342);
        ctx.textAlign = 'left';
      });

      // The provider's servers: a stack of three units with status lights
      const blink = 0.55 + 0.45 * Math.sin(t / 140);
      [-0.75, -0.2, 0.35].forEach((y, row) => {
        const face = box([SERVER_X, y + bob * 0.4, 0], [1.5, 0.42, 1.1], -0.5, C.server);
        onFace(face, 300, 84, () => {
          ctx.fillStyle = '#ece6f5';
          roundRect(ctx, 22, 24, 150, 10, 5);
          ctx.fill();
          roundRect(ctx, 22, 48, 110, 10, 5);
          ctx.fill();
          [0, 1, 2].forEach((i) => {
            ctx.globalAlpha = down ? blink : 1;
            ctx.fillStyle = down ? C.bad : C.ok;
            ctx.beginPath();
            ctx.arc(214 + i * 26, 42, 8, 0, Math.PI * 2);
            ctx.fill();
            ctx.globalAlpha = 1;
          });
          if (row === 0 && down) {
            ctx.strokeStyle = C.bad;
            ctx.lineWidth = 6;
            roundRect(ctx, 3, 3, 294, 78, 10);
            ctx.stroke();
          }
        });
      });

      // The payment travelling from the phone to the provider and back
      const from = [PHONE[0] + 0.6, 0.5 + bob, 0];
      const to = [SERVER_X - 0.85, 0.55, 0];
      const along = (k) => [from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k + 0.9 * Math.sin(Math.PI * k), 0];
      ctx.strokeStyle = C.path;
      ctx.lineWidth = 1.5;
      ctx.setLineDash([3, 6]);
      ctx.beginPath();
      for (let i = 0; i <= 40; i++) {
        const p = at(along(i / 40));
        i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y);
      }
      ctx.stroke();
      ctx.setLineDash([]);
      if (!verdict) {
        const k = returning ? 2 - trip * 2 : trip * 2;
        const p = at(along(k));
        const r = (compact ? 5 : 9) * p.k;
        ctx.fillStyle = !returning ? C.sun : down ? C.badBright : C.okBright;
        ctx.beginPath();
        ctx.arc(p.x, p.y, r, 0, Math.PI * 2);
        ctx.fill();
        if (returning) {
          ctx.strokeStyle = C.white;
          ctx.lineWidth = 2;
          ctx.beginPath();
          if (down) {
            ctx.moveTo(p.x - r * 0.4, p.y - r * 0.4);
            ctx.lineTo(p.x + r * 0.4, p.y + r * 0.4);
            ctx.moveTo(p.x + r * 0.4, p.y - r * 0.4);
            ctx.lineTo(p.x - r * 0.4, p.y + r * 0.4);
          } else {
            ctx.moveTo(p.x - r * 0.45, p.y);
            ctx.lineTo(p.x - r * 0.1, p.y + r * 0.35);
            ctx.lineTo(p.x + r * 0.45, p.y - r * 0.35);
          }
          ctx.stroke();
        }
      }

      // The KingaAPI shield, floating above, checks the provider
      const SHIELD = [0, 1.75 + bob, 0];
      const syaw = 0.5 * Math.sin((2 * Math.PI * t) / 7000);
      const thick = 0.09;
      const sp = (x, y, z) => {
        const r = rotY([x, y, z], syaw);
        return at([r[0] + SHIELD[0], r[1] + SHIELD[1], r[2] + SHIELD[2]]);
      };
      if (verdict) {
        const a = at(SHIELD);
        const b = at([SERVER_X - 0.3, 0.6, 0]);
        ctx.strokeStyle = C.badBright;
        ctx.lineWidth = 2;
        ctx.setLineDash([5, 5]);
        ctx.beginPath();
        ctx.moveTo(a.x, a.y);
        ctx.lineTo(b.x, b.y);
        ctx.stroke();
        ctx.setLineDash([]);
        const u = (phase % 900) / 900;
        ctx.fillStyle = C.white;
        ctx.beginPath();
        ctx.arc(a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u, 3, 0, Math.PI * 2);
        ctx.fill();
      }
      const front = outline.map(([x, y]) => sp(x, y, thick));
      const back = outline.map(([x, y]) => sp(x, y, -thick));
      poly(back, C.shieldBack);
      outline
        .map(([x, y], i) => {
          const j = (i + 1) % outline.length;
          const [x2, y2] = outline[j];
          const len = Math.hypot(x2 - x, y2 - y) || 1;
          const light = 0.5 + 0.5 * (-((y2 - y) / len) * Math.cos(syaw) - ((x2 - x) / len) * 0.4);
          return { quad: [front[i], front[j], back[j], back[i]], light, z: Math.sin(syaw) * (x + x2) };
        })
        .sort((p, q) => q.z - p.z)
        .forEach(({ quad, light }) => {
          const l = Math.max(0, Math.min(1, light));
          const c = C.sideDark.map((d, k) => Math.round(d + (C.sideLight[k] - d) * l));
          poly(quad, `rgb(${c[0]}, ${c[1]}, ${c[2]})`);
        });
      poly(front, C.shieldFront);
      ctx.strokeStyle = C.accent;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.lineWidth = 2.6 * SHIELD_UNIT * S;
      kLines.forEach((l) => {
        ctx.beginPath();
        l.forEach(([x, y], i) => {
          const p = sp(x, y, thick + 0.001);
          i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y);
        });
        ctx.stroke();
      });
      const d = sp(kDot[0], kDot[1], thick + 0.001);
      ctx.fillStyle = C.sun;
      ctx.beginPath();
      ctx.arc(d.x, d.y, 1.9 * SHIELD_UNIT * S, 0, Math.PI * 2);
      ctx.fill();

      // A few short labels so the scene explains itself
      if (!compact) {
        const phoneTop = at([PHONE[0], 1.35 + bob, 0]);
        pill(ctx, phoneTop.x, phoneTop.y, 'Your app', C.chip, C.chipText);
        const serverTop = at([SERVER_X, 0.95, 0]);
        if (down) pill(ctx, serverTop.x, serverTop.y - 6, 'Provider down', C.badBright, C.white);
        else pill(ctx, serverTop.x, serverTop.y - 6, 'Payment provider', C.chip, C.chipText);
        if (verdict) {
          const s = at([0, 2.55 + bob, 0]);
          pill(ctx, s.x, s.y, 'Not your code', C.white, C.ink);
        }
      }
    }

    const loop = (now) => {
      draw(now);
      frame = requestAnimationFrame(loop);
    };

    const observer = new ResizeObserver(resize);
    observer.observe(canvas);
    resize();
    if (!reduced) frame = requestAnimationFrame(loop);
    // Text uses IBM Plex Sans; redraw the still frame once the font is ready.
    document.fonts?.ready.then(() => reduced && draw(STILL));

    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, []);

  return <canvas ref={ref} className={className} aria-hidden="true" />;
}
