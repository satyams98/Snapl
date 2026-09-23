import { Link } from "react-router-dom";
import { BarChart3, Globe, KeyRound, Lock, MoonStar, ShieldCheck, SunMedium, Users, Zap } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useTheme } from "@/hooks/useTheme";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { DashboardPreviewMockup } from "@/components/marketing/DashboardPreviewMockup";

const FEATURES = [
  {
    icon: Globe,
    title: "Custom domains",
    description: "Brand every short link with a domain your customers already trust.",
    tint: "bg-primary-lighter text-primary",
  },
  {
    icon: BarChart3,
    title: "Real-time click analytics",
    description: "Kafka-backed pipelines stream click events into dashboards you can trust.",
    tint: "bg-info-lighter text-info-dark",
  },
  {
    icon: KeyRound,
    title: "Scoped API keys",
    description: "Automate link creation with read/write scoped keys built for CI and scripts.",
    tint: "bg-warning-lighter text-warning-dark",
  },
  {
    icon: Users,
    title: "Team roles",
    description: "Invite your team with Owner, Admin, and Member roles across one organization.",
    tint: "bg-primary-lighter text-primary",
  },
  {
    icon: Lock,
    title: "Password-protected & scheduled links",
    description: "Gate sensitive links with a password, or schedule them to go live later.",
    tint: "bg-success-lighter text-success-dark",
  },
  {
    icon: ShieldCheck,
    title: "Built to scale",
    description: "Redis-backed rate limiting and deduplication keep things fast under load.",
    tint: "bg-info-lighter text-info-dark",
  },
];

const PLANS = [
  {
    code: "FREE",
    name: "Free",
    price: "$0",
    description: "Get started with the essentials.",
    features: ["25 active links", "1,000 clicks / month", "No custom domains", "No API access"],
  },
  {
    code: "PRO",
    name: "Pro",
    price: "$29",
    description: "For growing teams that need more headroom.",
    features: ["1,000 active links", "50,000 clicks / month", "1 custom domain", "API access included"],
    highlighted: true,
  },
  {
    code: "BUSINESS",
    name: "Business",
    price: "$99",
    description: "Unlimited scale for serious link infrastructure.",
    features: ["Unlimited links", "Unlimited clicks", "5 custom domains", "API access included"],
  },
];

const STEPS = [
  { step: "1", title: "Create a link", description: "Shorten any URL, with an optional custom alias, password, or schedule." },
  { step: "2", title: "Share it", description: "Use your own branded domain so every click stays on-brand." },
  { step: "3", title: "Track it", description: "Watch clicks roll in on your dashboard, broken down by referrer and device." },
];

function ThemeToggleButton() {
  const { theme, toggle } = useTheme();
  return (
    <Button type="button" variant="ghost" size="icon" onClick={toggle} aria-label="Toggle theme">
      {theme === "dark" ? <SunMedium className="size-4" /> : <MoonStar className="size-4" />}
    </Button>
  );
}

export default function LandingPage() {
  const { user } = useAuth();

  return (
    <div className="min-h-svh bg-background">
      <header className="sticky top-0 z-30 border-b border-border/60 bg-card/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 lg:px-6">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Zap className="size-4" />
            </div>
            <span className="text-h6 font-medium">Snapl</span>
          </Link>
          <nav className="hidden items-center gap-6 text-body2 text-muted-foreground sm:flex">
            <a href="#features" className="hover:text-foreground">Features</a>
            <a href="#pricing" className="hover:text-foreground">Pricing</a>
          </nav>
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggleButton />
            {user ? (
              <Button asChild size="sm">
                <Link to="/dashboard">Go to Dashboard</Link>
              </Button>
            ) : (
              <>
                <Button asChild variant="ghost" size="sm">
                  <Link to="/login">Sign in</Link>
                </Button>
                <Button asChild size="sm">
                  <Link to="/register">Get started</Link>
                </Button>
              </>
            )}
          </div>
        </div>
      </header>

      <section className="relative overflow-hidden">
        <div
          className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[560px]"
          style={{
            background:
              "radial-gradient(ellipse 60% 55% at 50% -5%, var(--primary-lighter), transparent 70%)",
          }}
        />
        <div className="mx-auto flex max-w-6xl flex-col items-center gap-10 px-4 py-20 lg:flex-row lg:px-6">
        <div className="flex-1 space-y-6 text-center lg:text-left">
          <h1 className="text-h1 font-medium">Short links, built for teams that scale.</h1>
          <p className="text-body1 text-muted-foreground">
            Snapl gives your organization branded custom domains, real-time click analytics, and a
            scoped API — all behind role-based access for your whole team.
          </p>
          <div className="flex flex-col items-center gap-3 sm:flex-row lg:justify-start">
            <Button asChild size="lg">
              <Link to="/register">Start for free</Link>
            </Button>
            <Button asChild size="lg" variant="outline">
              <a href="#features">See what's included</a>
            </Button>
          </div>
        </div>
        <div className="flex flex-1 justify-center">
          <DashboardPreviewMockup />
        </div>
        </div>
      </section>

      <section id="features" className="mx-auto max-w-6xl px-4 py-16 lg:px-6">
        <div className="mb-10 text-center">
          <h2 className="text-h2 font-medium">Everything your team needs</h2>
          <p className="mt-2 text-body1 text-muted-foreground">Real infrastructure under the hood, not just a redirect.</p>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((feature) => (
            <Card
              key={feature.title}
              className="transition-all duration-200 hover:-translate-y-1 hover:shadow-lg"
            >
              <CardHeader>
                <div className={`mb-2 flex size-10 items-center justify-center rounded-lg ${feature.tint}`}>
                  <feature.icon className="size-5" />
                </div>
                <CardTitle>{feature.title}</CardTitle>
                <CardDescription>{feature.description}</CardDescription>
              </CardHeader>
            </Card>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 lg:px-6">
        <div className="mb-10 text-center">
          <h2 className="text-h2 font-medium">How it works</h2>
        </div>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
          {STEPS.map((item) => (
            <div key={item.step} className="text-center">
              <div className="mx-auto mb-3 flex size-10 items-center justify-center rounded-full bg-primary text-primary-foreground">
                {item.step}
              </div>
              <h3 className="text-h6 font-medium">{item.title}</h3>
              <p className="mt-1 text-body2 text-muted-foreground">{item.description}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="pricing" className="mx-auto max-w-6xl px-4 py-16 lg:px-6">
        <div className="mb-10 text-center">
          <h2 className="text-h2 font-medium">Simple, transparent pricing</h2>
          <p className="mt-2 text-body1 text-muted-foreground">Upgrade or downgrade any time.</p>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {PLANS.map((plan) => (
            <Card
              key={plan.code}
              className={
                plan.highlighted
                  ? "relative border-primary ring-1 ring-primary md:-translate-y-2"
                  : undefined
              }
              style={plan.highlighted ? { boxShadow: "0 20px 45px -18px rgba(43,49,133,0.45)" } : undefined}
            >
              {plan.highlighted && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-primary px-3 py-1 text-caption font-medium text-primary-foreground">
                  Most popular
                </span>
              )}
              <CardHeader>
                <CardTitle>{plan.name}</CardTitle>
                <CardDescription>{plan.description}</CardDescription>
                <p className="pt-2">
                  <span className="text-h3 font-medium">{plan.price}</span>
                  <span className="text-body2 text-muted-foreground">/mo</span>
                </p>
              </CardHeader>
              <CardContent className="space-y-3">
                <ul className="space-y-2 text-body2 text-muted-foreground">
                  {plan.features.map((feature) => (
                    <li key={feature}>{feature}</li>
                  ))}
                </ul>
                <Button asChild className="w-full" variant={plan.highlighted ? "default" : "outline"}>
                  <Link to="/register">Get started</Link>
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      <footer className="border-t border-border/60">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-4 py-8 text-body2 text-muted-foreground sm:flex-row lg:px-6">
          <span>© {new Date().getFullYear()} Snapl</span>
          <div className="flex gap-4">
            <Link to="/login" className="hover:text-foreground">Sign in</Link>
            <Link to="/register" className="hover:text-foreground">Create account</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
